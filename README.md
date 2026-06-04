# Amibee PDF Analyzer

## Overview

Amibee PDF Analyzer is a Spring Boot web application that allows users to analyze PDF documents using Artificial Intelligence. Users can provide a direct PDF URL, and the application downloads the PDF, extracts its text content, and generates an AI-powered summary and analysis using the Gemini API.

## Features

* Analyze PDF documents directly from URLs
* Automatic PDF download and validation
* Text extraction from PDF files
* AI-powered document summarization
* Error handling for invalid URLs and unsupported content
* Clean and responsive web interface
* REST-based backend architecture

## Technology Stack

### Backend

* Java 21
* Spring Boot
* Maven

### AI Integration

* Google Gemini API

### PDF Processing

* Apache PDFBox

### Frontend

* HTML
* CSS
* Thymeleaf

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── dto/
│   │   └── exception/
│   └── resources/
│       ├── templates/
│       ├── static/
│       └── application.properties
└── test/
```

## How It Works

1. User enters a PDF URL.
2. The application validates the URL.
3. The PDF is downloaded.
4. Text content is extracted from the PDF.
5. Extracted content is sent to Gemini AI.
6. AI generates a summary and analysis.
7. Results are displayed to the user.

## Installation

### Clone the Repository

```bash
git clone https://github.com/Amrinhaq/Amibee-PDF-ANALYZER.git
```

### Navigate to the Project

```bash
cd Amibee-PDF-ANALYZER
```

### Configure Gemini API Key

Open `application.properties` and configure:

```properties
gemini.api.key=YOUR_API_KEY
```

### Run the Application

```bash
mvn spring-boot:run
```

The application will start at:

```text
http://localhost:8080
```

## Exception Handling

The application includes custom exception handling for:

* Invalid URL format
* URL not found
* Empty PDF files
* Scanned or image-based PDFs
* PDF size limits
* Network failures
* Gemini API failures
* Unexpected processing errors


## Author

Amrin A

Computer Science Engineering Student

Passionate about Java, Spring Boot, AI Applications, and Full Stack Development.
