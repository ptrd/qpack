package net.luminis.qpack;

import net.luminis.qpack.impl.EncoderImpl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface Encoder {

    ByteBuffer compressHeaders(List<Map.Entry<String, String>> headers);

    interface Builder {
        Encoder build();
    }

    static Builder newBuilder() {
        return new Builder() {
            @Override
            public Encoder build() {
                return new EncoderImpl();
            }
        };
    }
}
