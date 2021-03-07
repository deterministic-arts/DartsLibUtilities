package darts.lib.util.serialization.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import darts.lib.util.OctetString;
import darts.lib.util.Pair;

public final class Module extends SimpleModule {
    public Module() {
        addSerializer(OctetString.class, new OctetStringSerializer());
        addDeserializer(OctetString.class, new OctetStringDeserializer());
        addSerializer(Pair.class, new PairSerializer());
    }
}
