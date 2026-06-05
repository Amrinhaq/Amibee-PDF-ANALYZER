package com.example.pdfanalyzer.service;

import com.example.pdfanalyzer.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service to handle downloading PDFs from user-provided URLs.
 * Integrates custom exception handling to protect and map system operations.
 */
@Service
@Slf4j
public class PdfDownloadService {

    private static final int MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_REDIRECT_HOPS = 5;

    private final HttpClient httpClient;

    public PdfDownloadService() {
        // Disable automatic client-side redirects so we can inspect and security-check intermediate URLs manually
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Downloads a PDF from a URL.
     * Follows redirect chains safely, but throws RedirectedUrlException if the final resolved destination is not a PDF.
     */
    public byte[] downloadPdf(String urlString) {
        log.info("Downloading PDF from URL: {}", urlString);
        
        // 1. Google Drive Folder Link Detection
        if (urlString != null && urlString.contains("drive.google.com/drive/folders/")) {
            log.warn("Blocked Google Drive folder URL: {}", urlString);
            throw new InvalidContentException("This URL points to a Google Drive folder, not a direct PDF file. Please share the specific PDF file and copy its link.");
        }

        // 2. Auto-convert Google Drive file viewer URLs to direct download links
        if (urlString != null && urlString.contains("drive.google.com/file/d/")) {
            String convertedUrl = convertDriveUrl(urlString);
            log.info("Auto-converted Google Drive share URL to direct download URL: {} -> {}", urlString, convertedUrl);
            urlString = convertedUrl;
        }

        int redirectCount = 0;
        String currentUrl = urlString;

        try {
            while (redirectCount < MAX_REDIRECT_HOPS) {
                URI uri = new URI(currentUrl).normalize();
                URL url = uri.toURL();

                // Validate protocol is HTTP/HTTPS
                String protocol = url.getProtocol();
                if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                    throw new InvalidUrlFormatException();
                }

                // Validate host exists and block internal network endpoints (SSRF Check)
                String host = uri.getHost();
                if (host == null || host.isEmpty()) {
                    throw new InvalidUrlFormatException();
                }

                InetAddress inetAddress;
                try {
                    inetAddress = InetAddress.getByName(host);
                } catch (Exception e) {
                    log.error("DNS Resolution failed for host: {}", host, e);
                    throw new NetworkFailureException("Host resolution failed", e);
                }

                if (inetAddress.isLoopbackAddress() || inetAddress.isSiteLocalAddress() || inetAddress.isLinkLocalAddress()) {
                    log.warn("Blocked request to internal host: {} ({})", host, inetAddress.getHostAddress());
                    throw new InvalidUrlFormatException();
                }

                // Create request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) PDF-Analyzer-Service")
                        .GET()
                        .build();

                // Send request
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                int statusCode = response.statusCode();

                // Intercept HTTP redirects (301, 302, 303, 307, 308)
                // Note: 303 is commonly used by Google Drive API redirects
                if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null || location.isEmpty()) {
                        throw new RedirectedUrlException();
                    }

                    // Resolve relative/absolute URL redirects
                    URI resolvedUri = uri.resolve(location);
                    currentUrl = resolvedUri.toString();
                    redirectCount++;
                    log.info("Following redirect hop #{} to: {}", redirectCount, currentUrl);
                    continue;
                }

                // Handle URL Not Found
                if (statusCode == 404) {
                    log.warn("URL returned 404 Not Found");
                    throw new UrlNotFoundException();
                }

                // Handle other HTTP errors
                if (statusCode != 200) {
                    log.warn("Server returned HTTP error status: {}", statusCode);
                    throw new NetworkFailureException("HTTP error " + statusCode, null);
                }

                // Validate Content-Type
                // Support application/octet-stream and application/binary since Google Drive serves direct file downloads as binary streams
                String contentType = response.headers().firstValue("Content-Type").orElse(null);
                if (contentType != null) {
                    contentType = contentType.toLowerCase();
                }
                
                boolean isValidPdf = contentType != null && 
                                     (contentType.startsWith("application/pdf") || 
                                      contentType.startsWith("application/octet-stream") || 
                                      contentType.startsWith("application/binary"));
                
                if (!isValidPdf) {
                    log.warn("Incorrect Content-Type received: {}", contentType);
                    if (redirectCount > 0) {
                        // The URL did redirect, but resulted in a non-PDF (like a landing page)
                        throw new RedirectedUrlException();
                    } else {
                        // No redirects occurred, but it's not a PDF
                        throw new InvalidContentException();
                    }
                }

                // Validate Content-Length
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                if (contentLength > MAX_FILE_SIZE_BYTES) {
                    log.warn("Content-Length exceeds 10MB: {}", contentLength);
                    throw new PdfTooLargeException();
                }

                byte[] body = response.body();
                if (body == null || body.length == 0) {
                    throw new EmptyPdfException();
                }

                if (body.length > MAX_FILE_SIZE_BYTES) {
                    log.warn("Downloaded bytes exceed 10MB: {}", body.length);
                    throw new PdfTooLargeException();
                }

                return body;
            }

            // Exceeded hop count
            throw new RedirectedUrlException();

        } catch (PdfAnalysisException e) {
            throw e;
        } catch (java.net.URISyntaxException | java.net.MalformedURLException | IllegalArgumentException e) {
            log.error("URL parsing syntax error: {}", urlString, e);
            throw new InvalidUrlFormatException();
        } catch (HttpConnectTimeoutException | ConnectException e) {
            log.error("Network timeout/connection refused for URL: {}", urlString, e);
            throw new NetworkFailureException("Timeout", e);
        } catch (IOException e) {
            log.error("I/O error downloading PDF from URL: {}", urlString, e);
            throw new NetworkFailureException("I/O failure", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download thread interrupted", e);
            throw new UnexpectedErrorException("Interrupted", e);
        } catch (Exception e) {
            log.error("Unexpected error in downloader: ", e);
            throw new UnexpectedErrorException("General error", e);
        }
    }

    /**
     * Converts a standard Google Drive file sharing/viewing link into a direct download API URL.
     * Example: https://drive.google.com/file/d/FILE_ID/view?usp=sharing
     * -> https://drive.google.com/uc?export=download&id=FILE_ID
     */
    private String convertDriveUrl(String url) {
        try {
            int startIndex = url.indexOf("/file/d/") + "/file/d/".length();
            int endIndex = url.indexOf("/", startIndex);
            if (endIndex == -1) {
                endIndex = url.indexOf("?", startIndex);
            }
            String fileId = (endIndex != -1) ? url.substring(startIndex, endIndex) : url.substring(startIndex);
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        } catch (Exception e) {
            log.error("Failed to parse Google Drive file URL: {}", url, e);
            return url;
        }
    }
}
