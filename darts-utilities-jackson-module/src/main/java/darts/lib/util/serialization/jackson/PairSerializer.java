package darts.lib.util.serialization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import darts.lib.util.Pair;

import java.io.IOException;

@SuppressWarnings({"rawtypes"})
public final class PairSerializer extends StdSerializer<Pair> {
    public PairSerializer() {
        super(Pair.class);
    }
    @Override
    public void serialize(Pair value, JsonGenerator gen, SerializerProvider provider)
    throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("first");
        gen.writeObject(value.first());
        gen.writeFieldName("second");
        gen.writeObject(value.second());
        gen.writeEndObject();
    }
}
