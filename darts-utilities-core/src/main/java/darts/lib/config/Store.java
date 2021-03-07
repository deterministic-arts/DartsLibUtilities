package darts.lib.config;

import darts.lib.util.Traversable;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * A storage of configuration settings. An instance of
 * this interface is essentially a mapping from setting
 * names to values, both of which are strings. In addition
 * to this basic behaviour, a store also provides the
 * capabilities of expanding strings with embedded
 * substitution markers.
 */

@SuppressWarnings("SameReturnValue")
public interface Store {

    /**
     * Looks up the value of the given key in this store. If
     * no matching value exists, answers an empty result. Does
     * not interpret the key and/or the value in any way. In
     * particular, does not perform any expansion of the value.
     *
     * @param key   setting to look up
     *
     * @return  the value of the named setting or an an empty
     *          result
     */

    Optional<String> query(String key);

    /**
     * Answers a traversable, that yields all key/value pairs
     * accessible via this store. All keys are unique, and in case
     * of composite stores, only the values accessible via {@link #query(String)}
     * are produced by the enumeration.
     *
     * @return a traversable, that produces all key/value pairs in this store
     */
    Traversable<Map.Entry<String, String>> enumerate();

    // region Derived Look Up And Expansion

    default Try<String> tryGet(String name) {
        return query(name).map(Try::success).orElseGet(() -> Try.failure(new NoSuchElementException(name)));
    }

    default Try<String> tryExpand(String template) {
        return Try.ofSupplier(() -> Expander.expand(template, Expander.forStore(this, "")));
    }

    /**
     * Like {@link #query(String)}, but assumes, that the
     * setting with the given name actually exists. Throws an
     * exception, if no matching setting is found.
     *
     * @param key   setting to look up
     *
     * @return  the value associated with the given setting
     */

    default String get(String key) {
        return tryGet(key).get();
    }

    /**
     * Expands the string {@code template}, answering a copy,
     * in which occurrences of substitution markers have been
     * replaced by their values. A substitution marker has the
     * general form
     *
     * <blockquote><code>${<i>key</i>}</code></blockquote>
     *
     * <p>where <code><i>key</i></code> is the name of a
     * setting contained in this store. If the value associated
     * with <code><i>key</i></code> contains substitution
     * markers, those are replaced with their values recursively.
     * An implementation may impose arbitrary limits on the
     * level of recursive expansions made; this limit must be
     * no lower than at least 5 levels of recursive expansions.
     *
     * @param template  template string to expand
     *
     * @return  a copy of the input string with substitution
     *          markers replaced by their values.
     */

    default String expand(String template) {
        return tryExpand(template).get();
    }

    // endregion

    // region Composition

    /**
     * Answers a store, which looks up all settings in this
     * store. If this store has no matching entry for a requested
     * key, the result store also looks into {@code defaults}
     * before giving up.
     *
     * @param defaults  store to provide default values
     *
     * @return  a composite store
     */

    default Store compose(Store defaults) {
        return StoreSupport.cons(this, Objects.requireNonNull(defaults));
    }

    // endregion

    // region Construction

    /**
     * Answers a store, that has no contents. All look ups will
     * fail.
     */

    static Store empty() {
        return StoreSupport.EMPTY;
    }

    /**
     * Answers a store, which is backed by the JVM's system
     * properties.
     */

    static Store system() {
        return StoreSupport.SYSTEM;
    }

    /**
     * Answers a store, that is backed by the JVM process'
     * global environment
     */

    static Store environment() {
        return StoreSupport.ENVIRONMENT;
    }

    /**
     * Constructs a store with a known set of settings.
     *
     * @param map   map from strings (keys) to values (settings)
     *
     * @return  a store, which is backed by the given mapping
     */

    static Store ofAll(io.vavr.collection.Map<String, String> map) {
        return new StoreSupport.VavrMapped(Objects.requireNonNull(map));
    }

    /**
     * Answers a store with a known set of settings. The method
     * will copy the given map if necessary to ensure, that
     * subsequent modifications made to it do not affect the
     * store returned.
     *
     * @param map   contents of the new store
     *
     * @return  a store, which has exactly the contents of the
     *          given mapping
     */

    static Store ofAll(Map<String, String> map) {
        return new StoreSupport.VavrMapped(io.vavr.collection.HashMap.ofAll(map));
    }

    static Store of(String k1, String v1) {
        return ofAll(io.vavr.collection.HashMap.of(k1, v1));
    }

    static Store of(String k1, String v1, String k2, String v2) {
        return ofAll(io.vavr.collection.HashMap.of(k1, v1, k2, v2));
    }

    static Store of(String k1, String v1, String k2, String v2, String k3, String v3) {
        return ofAll(io.vavr.collection.HashMap.of(k1, v1, k2, v2, k3, v3));
    }

    static Store of(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String... rest) {
        io.vavr.collection.HashMap<String, String> map = io.vavr.collection.HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        for (int p = 0; p < rest.length; ) {
            map = map.put(rest[p], rest[p + 1]);
            p += 2;
        }
        return ofAll(map);
    }

    // endregion

    // region Loading

    static Store load(String src, Reader reader) throws IOException {
        return ofAll(Parser.parse(src, reader, io.vavr.collection.HashMap.empty()));
    }

    static Store load(File source) throws IOException {
        return ofAll(Parser.parse(source));
    }

    static Store load(URI source) throws IOException {
        return ofAll(Parser.parse(source));
    }

    static Store load(URL source) throws IOException {
        return ofAll(Parser.parse(source));
    }

    // endregion
}
