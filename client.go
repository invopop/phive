package phive

import (
	"context"
	"fmt"

	"github.com/invopop/phive/proto"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// Client wraps the gRPC ValidationService client with convenience methods
type Client struct {
	conn   *grpc.ClientConn
	client proto.ValidationServiceClient
}

// NewClient creates a new Phive validation client
func NewClient(address string, opts ...grpc.DialOption) (*Client, error) {
	if len(opts) == 0 {
		opts = []grpc.DialOption{grpc.WithTransportCredentials(insecure.NewCredentials())}
	}

	conn, err := grpc.NewClient(address, opts...)
	if err != nil {
		return nil, fmt.Errorf("failed to connect: %w", err)
	}

	return &Client{
		conn:   conn,
		client: proto.NewValidationServiceClient(conn),
	}, nil
}

// Close closes the client connection
func (c *Client) Close() error {
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

// ListVesIds returns all available validation rule sets, optionally filtered by keyword
func (c *Client) ListVesIds(ctx context.Context, filter string) (*proto.ListVesIdsResponse, error) {
	return c.client.ListVesIds(ctx, &proto.ListVesIdsRequest{
		Filter: filter,
	})
}

// ValidateXML validates XML content against a specific VESID
func (c *Client) ValidateXML(ctx context.Context, vesid string, xmlContent []byte, sourceID string) (*proto.ValidateXmlResponse, error) {
	return c.client.ValidateXml(ctx, &proto.ValidateXmlRequest{
		Vesid:            vesid,
		XmlContent:       xmlContent,
		SourceIdentifier: sourceID,
	})
}

// Client returns the underlying gRPC client for advanced usage
func (c *Client) Client() proto.ValidationServiceClient {
	return c.client
}
