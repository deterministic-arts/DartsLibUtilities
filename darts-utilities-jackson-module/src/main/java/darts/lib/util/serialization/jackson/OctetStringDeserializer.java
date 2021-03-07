package darts.lib.util.serialization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import darts.lib.util.OctetString;

import java.io.IOException;

final class OctetStringDeserializer extends StdDeserializer<OctetString> {
    public OctetStringDeserializer() {
        super(OctetString.class);
    }

    @Override
    public OctetString deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
    throws IOException, JsonProcessingException {
        switch (jsonParser.getCurrentToken()) {
        case VALUE_NULL:
            jsonParser.nextToken();
            return null;
        case VALUE_STRING:
            final var data = jsonParser.getValueAsString();
            final OctetString result;
            try {
                result = OctetString.fromString(data);
            } catch (IllegalArgumentException exc) {
                deserializationContext.handleWeirdStringValue(OctetString.class, data, "malformed octet string");
                throw new IllegalStateException("not reached");
            }
            jsonParser.nextToken();
            return result;
        }
        return (OctetString) deserializationContext.handleUnexpectedToken(OctetString.class, jsonParser);
    }
}
