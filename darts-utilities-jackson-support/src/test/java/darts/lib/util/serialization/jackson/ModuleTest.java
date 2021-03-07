package darts.lib.util.serialization.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import darts.lib.util.OctetString;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ModuleTest {

    @Test
    public void octetString() throws Exception {
        final var mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        final var string = OctetString.of(0, 1, 2, 3);
        final var encoded = mapper.writeValueAsString(string);
        final var decoded = mapper.readValue(encoded, OctetString.class);

        assertEquals(string, decoded);
    }

    @Test
    public void containedOctetString() throws Exception {
        final var mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        final var string = OctetString.of(0, 1, 2, 3);
        final var encoded = mapper.writeValueAsString(Collections.singletonList(new Container(string)));
        final List<Container> decoded = mapper.readValue(encoded, new TypeReference<List<Container>>(){});

        assertEquals(string, decoded.get(0).content);
    }

    static final class Container {

        OctetString content;

        @JsonCreator
        Container(@JsonProperty("content") OctetString c) {
            content = c;
        }

        @JsonProperty
        public OctetString getContent() {
            return content;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Container container = (Container) o;
            return content != null ? content.equals(container.content) : container.content == null;
        }

        @Override
        public int hashCode() {
            return content != null ? content.hashCode() : 0;
        }
    }
}