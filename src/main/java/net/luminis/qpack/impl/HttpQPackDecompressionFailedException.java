package net.luminis.qpack.impl;


// https://tools.ietf.org/html/draft-ietf-quic-qpack-07#section-3.1
// "When the decoder encounters an invalid static table index in a header
//   block instruction it MUST treat this as a stream error of type
//   "HTTP_QPACK_DECOMPRESSION_FAILED"."
public class HttpQPackDecompressionFailedException extends RuntimeException {
}
