package darts.lib.util.serialization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import darts.lib.util.OctetString;

import java.io.IOException;

final class OctetStringSerializer extends StdSerializer<OctetString> {
    public OctetStringSerializer() {
        super(OctetString.class);
    }

    @Override
    public void serialize(OctetString integers, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
    throws IOException {
        jsonGenerator.writeString(integers.toString());
    }
}
