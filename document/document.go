package document

import (
	"strconv"
)

type SmithyDocumentMarshaler interface {
	MarshalSmithyDocument() ([]byte, error)
}

type SmithyDocumentUnmarshaler interface {
	UnmarshalSmithyDocument(interface{}) error
}

type noSerde interface {
	noSmithyDocumentSerde()
}

// NoSerde is a sentinel value to indicate that a given type should not be marshaled or unmarshalled
// into a protocol document.
type NoSerde struct{}

func (n NoSerde) noSmithyDocumentSerde() {}

var _ noSerde = (*NoSerde)(nil)

// IsNoSerde returns whether the given type implements the no smithy document serde interface.
func IsNoSerde(x interface{}) bool {
	_, ok := x.(noSerde)
	return ok
}

// Number is a arbitrary precision numerical value
type Number string

// Int64 returns the number as a string.
func (n Number) String() string {
	return string(n)
}

// Int64 returns the number as an int64.
func (n Number) Int64() (int64, error) {
	return n.intOfBitSize(64)
}

func (n Number) intOfBitSize(bitSize int) (int64, error) {
	return strconv.ParseInt(string(n), 10, bitSize)
}

func (n Number) Uint64() (uint64, error) {
	return n.uintOfBitSize(64)
}

func (n Number) uintOfBitSize(bitSize int) (uint64, error) {
	return strconv.ParseUint(string(n), 10, bitSize)
}

// Float32 returns the number parsed as a 32-bit float, returns a float64.
func (n Number) Float32() (float64, error) {
	return n.floatOfBitSize(32)
}

// Float64 returns the number as a float64.
func (n Number) Float64() (float64, error) {
	return n.floatOfBitSize(64)
}

// Float64 returns the number as a float64.
func (n Number) floatOfBitSize(bitSize int) (float64, error) {
	return strconv.ParseFloat(string(n), bitSize)
}
