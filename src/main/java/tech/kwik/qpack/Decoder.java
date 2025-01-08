package tech.kwik.qpack;

import tech.kwik.qpack.impl.DecoderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface Decoder {

    List<Map.Entry<String, String>> decodeStream(InputStream inputStream) throws IOException;

    interface Builder {
        Decoder build();
    }

    static Builder newBuilder() {
        return new Builder() {
            @Override
            public Decoder build() {
                return new DecoderImpl();
            }
        };
    }

}
