package darts.lib.config;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class StoreTest {

    @Test
    public void multiLevelFlatExpansion() {
        final Store store = Store.of("a", "a${b}", "b", "b${c}", "c", "c");
        assertEquals("abc", store.expand("${a}"));
    }

    @Test
    public void multiLevelNestedExpansion() {
        final Store store = Store.of("app.host", "localhost", "app.db.host", "${..host}", "app.db.port", "5432", "app.db.url", "jdbc:postgresql://${.host}:${.port}/database");
        assertEquals("jdbc:postgresql://localhost:5432/database", store.expand("${app.db.url}"));
    }

    @Test
    public void expand_resolvesSubstitutions() throws Exception {
        final Store store = Store.load(getClass().getResource("/test.nprop"));
        assertEquals("jdbc:postgresql://localhost:5432/coffee", store.expand("${coffee.db.url}"));
    }

    @Test
    public void enumerate_generatesUniqueKeysOnly() {
        final Store s1 = Store.of("k1", "va", "k2", "va");
        final Store s2 = Store.of("k2", "vb", "k3", "vb");
        final Store combi = s1.compose(s2);
        final Set<Map.Entry<String, String>> all = combi.enumerate().collect(Collectors.toSet());
        assertEquals(Set.of(entry("k1", "va"), entry("k2", "va"), entry("k3", "vb")), all);
    }

    private static Map.Entry<String, String> entry(String k, String v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }
}
