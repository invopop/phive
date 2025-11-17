# Phive gRPC Service

A gRPC wrapper for [phive](https://github.com/phax/phive) (Philip Helger Integrative Validation Engine) that validates XML documents against e-invoicing standards.

Built with ❤️ by [Invopop](https://invopop.com) using the excellent [phive](https://github.com/phax/phive) and [phive-rules](https://github.com/phax/phive-rules) libraries by [@phax](https://github.com/phax).

## What it does

- **List VESIDs**: Get all available validation rule sets (105+ included)
- **Validate XML**: Validate invoices and documents against standards like Peppol, EN 16931, XRechnung, UBL, CII, and more

## Quick Start

```bash
# Start the service
docker run -d -p 9090:9090 invopop/phive-grpc-service

# List all available validation rule sets
grpcurl -plaintext -d '{"filter":""}' localhost:9090 invopop.phive.v1.ValidationService/ListVesIds

# Filter by keyword
grpcurl -plaintext -d '{"filter":"peppol"}' localhost:9090 invopop.phive.v1.ValidationService/ListVesIds

# Validate XML (using proto file)
grpcurl -plaintext \
  -import-path src/main/proto \
  -proto validation.proto \
  -d '{
    "vesid": "eu.peppol.bis3:invoice:2024.5",
    "xml_content": "'$(base64 < invoice.xml)'"
  }' \
  localhost:9090 invopop.phive.v1.ValidationService/ValidateXml
```

## API

**Service:** `invopop.phive.v1.ValidationService`

### ListVesIds
Get available validation rule sets with optional filtering.

**Request:**
```json
{
  "filter": "peppol"  // optional keyword filter
}
```

**Response:**
```json
{
  "vesids": [
    {
      "vesid": "eu.peppol.bis3:invoice:2024.5",
      "name": "OpenPeppol Invoice (2024.5)",
      "version": "2024.5",
      "status": "valid"
    }
  ]
}
```

### ValidateXml
Validate an XML document against a specific VESID.

**Request:**
```json
{
  "vesid": "eu.peppol.bis3:invoice:2024.5",
  "xml_content": "<base64-encoded-xml>",
  "source_identifier": "optional-doc-id"
}
```

**Response:**
```json
{
  "success": true,
  "resolved_vesid": "eu.peppol.bis3:invoice:2024.5",
  "timestamp": "2025-01-17T23:38:59Z",
  "results": [
    {
      "validation_type": "XSD",
      "artifact_id": "path/to/schema.xsd",
      "success": true,
      "errors": [],
      "warnings": []
    }
  ]
}
```

## Building

```bash
# Build with Maven
mvn clean package

# Build Docker image
docker build -t phive-grpc-service .
```

## Configuration

Set environment variables:
- `GRPC_SERVER_PORT`: gRPC server port (default: 9090)
- `JAVA_OPTS`: JVM options (default: `-Xms256m -Xmx1024m`)

## Included Validation Rules

- **Peppol BIS 3** - Peppol Business Interoperability Specifications
- **EN 16931** - European e-invoicing standard
- **XRechnung** - German e-invoicing
- **UBL** - Universal Business Language
- **CII** - Cross Industry Invoice
- **FacturaE** - Spanish e-invoicing
- **FatturaPA** - Italian e-invoicing

Total: **105+ validation rule sets**

See [phive-rules](https://github.com/phax/phive-rules) for the complete list.

## License

Apache License 2.0

## Links

- [phive](https://github.com/phax/phive) - Core validation engine
- [phive-rules](https://github.com/phax/phive-rules) - Pre-built validation rules
- [Invopop](https://invopop.com) - Electronic invoicing solutions
