# Getting started

## Run using Docker
Clone the project and run:
```bash
$ ./run.sh
```

# API Documentation

## Overview

This API provides functionality for ontology-related operations, including class inference and OWL to SHACL conversion. It exposes two endpoints:

1. `/api/types` (POST): Infers classes based on input data, schema, and target, returning a list of inferred classes in JSON format.
2. `/api/owl2shacl` (GET): Converts an OWL ontology from a given URL to SHACL, responding in either Turtle or JSON format based on the `Accept` header.

## Endpoints

### POST `/api/types`

Infers ontology classes based on the input provided in the request body. The service processes the data, schema, and target to return a JSON array of inferred classes.

#### Request Specification
- **Body**: JSON object with the following fields:
  - `data` (String): Input data for analysis.
  - `schema` (String): Schema to use for class inference.
  - `target` (String): Target entity for which to infer classes.
  
- **Example Request**:
  ```json
  {
    "data": "example data",
    "schema": "example schema",
    "target": "example target"
  }
  ```

#### Response Specification
- **Content-Type**: `application/json`
- **Body**: A JSON array of inferred classes.

- **Example Response**:
  ```json
  [
    "Class1",
    "Class2",
    "Class3"
  ]
  ```

---

### GET `/api/owl2shacl`

Converts an OWL ontology from a provided URL to SHACL. The response format depends on the `Accept` header in the request.

#### Request Specification
- **Query Parameter**:
  - `url` (String, required): The URL of the OWL ontology to convert.
  
- **Headers**:
  - `Accept`:
    - `text/turtle`: Responds with SHACL in Turtle format.
    - `application/json`: Responds with SHACL in JSON format.
  
- **Example Request**:
  ```
  GET /api/owl2shacl?url=http://example.com/ontology.owl
  Accept: application/json
  ```

#### Response Specification
- **Content-Type**: 
  - `text/turtle` for Turtle format.
  - `application/json` for JSON format.
  
- **Body**: 
  - For `text/turtle`, the response is a plain text serialization of the SHACL.
  - For `application/json`, the response is a JSON object containing the SHACL result as a value for the `result` key.

- **Example Response** (JSON):
  ```json
  {
    "result": "Serialized SHACL in Turtle format as a single string"
  }
  ```
