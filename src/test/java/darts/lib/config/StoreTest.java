package darts.lib.config;

import org.junit.Assert;
import org.junit.Test;

public class StoreTest {

    @Test
    public void multiLevelFlatExpansion() {
        final Store store = Store.of("a", "a${b}", "b", "b${c}", "c", "c");
        Assert.assertEquals("abc", store.expand("${a}"));
    }

    @Test
    public void multiLevelNestedExpansion() {
        final Store store = Store.of("app.host", "localhost", "app.db.host", "${..host}", "app.db.port", "5432", "app.db.url", "jdbc:postgresql://${.host}:${.port}/database");
        Assert.assertEquals("jdbc:postgresql://localhost:5432/database", store.expand("${app.db.url}"));
    }

    @Test
    public void tooMuch() throws Exception {
        final Store store = Store.load(getClass().getResource("/test.nprop"));
        Assert.assertEquals("jdbc:postgresql://localhost:5432/coffee", store.expand("${coffee.db.url}"));
    }
}
