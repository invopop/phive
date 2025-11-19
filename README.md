# Phive gRPC Service

A gRPC wrapper for [phive](https://github.com/phax/phive) (Philip Helger Integrative Validation Engine) that validates XML documents against e-invoicing standards.

Built using the [phive](https://github.com/phax/phive) and [phive-rules](https://github.com/phax/phive-rules) libraries by [@phax](https://github.com/phax).

## What it does

- **List VESIDs**: Get all available validation rule sets (470+ included)
- **Validate XML**: Validate invoices and documents against standards like Peppol, EN 16931, XRechnung, UBL, CII, and more

**Note:** Many validation sets are marked as `deprecated` - filter by `status: "valid"` to get current rule sets.

## Quick Start

### Using Docker

```bash
# Start the service
docker run -d -p 9090:9090 invopop/phive-grpc-service
```

### Using Go Client

```go
package main

import (
	"context"
	"fmt"
	"log"

	"github.com/invopop/phive"
)

func main() {
	// Create client
	client, err := phive.NewClient("localhost:9090")
	if err != nil {
		log.Fatal(err)
	}
	defer client.Close()

	ctx := context.Background()

	// List validation rule sets
	resp, err := client.ListVesIds(ctx, "peppol")
	if err != nil {
		log.Fatal(err)
	}

	for _, vesid := range resp.Vesids {
		if vesid.Status == "valid" {
			fmt.Printf("%s: %s\n", vesid.Vesid, vesid.Name)
		}
	}

	// Validate XML
	xmlContent := []byte(`<xml>...</xml>`)
	result, err := client.ValidateXML(ctx,
		"eu.peppol.bis3:invoice:2024.5",
		xmlContent,
		"my-invoice-id",
	)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Printf("Valid: %v\n", result.Success)
}
```

### Using grpcurl

```bash
# Clone repo to get proto file
git clone https://github.com/invopop/phive.git
cd phive

# List all available validation rule sets
grpcurl -plaintext \
  -import-path proto \
  -proto validation.proto \
  -d '{"filter":""}' \
  localhost:9090 invopop.phive.v1.ValidationService/ListVesIds

# Filter by keyword
grpcurl -plaintext \
  -import-path proto \
  -proto validation.proto \
  -d '{"filter":"peppol"}' \
  localhost:9090 invopop.phive.v1.ValidationService/ListVesIds

# Validate XML (use -w 0 to prevent newlines in base64 output)
grpcurl -plaintext \
  -import-path proto \
  -proto validation.proto \
  -d '{
    "vesid": "un.unece.uncefact:crossindustryinvoice:D22B",
    "xml_content": "'$(base64 -w 0 < invoice.xml)'"
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

## Proto Files

The proto definitions are available in the `proto/` directory and can be used directly in any language:

```bash
# For Go
protoc --go_out=. --go_opt=paths=source_relative \
  --go-grpc_out=. --go-grpc_opt=paths=source_relative \
  proto/validation.proto
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
- **UBL** - Universal Business Language (all versions)
- **CII** - Cross Industry Invoice (D16B, D22B)
- **FacturaE** - Spanish e-invoicing
- **FatturaPA** - Italian e-invoicing
- **ZUGFeRD** - German e-invoicing format
- **Factur-X** - French e-invoicing


Many rule sets are deprecated.

See [phive-rules](https://github.com/phax/phive-rules) for the complete list.

## License

Apache License 2.0

## Links

- [phive](https://github.com/phax/phive) - Core validation engine
- [phive-rules](https://github.com/phax/phive-rules) - Pre-built validation rules
