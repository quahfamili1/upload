# OpenMetadata Sidecar

This is a Spring Boot microservice that allows you to upload a CSV file and create a corresponding table in OpenMetadata.

## Prerequisites

- Java 11
- Maven
- A running instance of OpenMetadata at `http://localhost:8585`

## Build and Run

1.  **Build the application:**

    ```bash
    ./mvnw clean install
    ```

2.  **Run the application:**

    ```bash
    java -jar target/openmetadata-sidecar-0.0.1-SNAPSHOT.jar
    ```

## Usage

Send a POST request to `http://localhost:8080/api/v1/upload` with the following parameters:

-   `file`: The CSV file to upload.
-   `service`: The OpenMetadata service name.
-   `databaseSchema`: The OpenMetadata database schema name.

### Example using cURL

```bash
curl -X POST -F "file=@/path/to/your/file.csv" -F "service=your_service" -F "databaseSchema=your_schema" http://localhost:8080/api/v1/upload
```
