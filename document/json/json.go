package json

func NewEncoder(optFns ...func(options *EncoderOptions)) *Encoder {
	o := EncoderOptions{}

	for _, fn := range optFns {
		fn(&o)
	}

	return &Encoder{
		options: o,
	}
}

func NewDecoder(optFns ...func(*DecoderOptions)) *Decoder {
	o := DecoderOptions{}

	for _, fn := range optFns {
		fn(&o)
	}

	return &Decoder{
		options: o,
	}
}
