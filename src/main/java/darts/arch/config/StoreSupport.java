package darts.arch.config;

import darts.arch.util.Traversable;
import io.vavr.Tuple2;

import java.util.*;
import java.util.function.BiFunction;

enum StoreSupport implements Store {

    SYSTEM {
        @Override
        public Optional<String> query(String key) {
            return Optional.ofNullable(System.getProperty(key));
        }
        @Override
        public Traversable<Map.Entry<String, String>> enumerate() {
            return new Traversable<Map.Entry<String, String>>() {
                @Override
                public <M> M fold(M seed, BiFunction<? super M, ? super Map.Entry<String, String>, ? extends M> fn) {
                    for (Map.Entry<?, ?> e: System.getProperties().entrySet()) {
                        final var k = e.getKey();
                        if (k instanceof String) {
                            final var v = e.getValue();
                            if (v instanceof String) {
                                seed = fn.apply(seed, new AbstractMap.SimpleImmutableEntry<>((String)k, (String) v));
                            }
                        }
                    }
                    return seed;
                }
            };
        }
    },
    ENVIRONMENT {
        @Override
        public Optional<String> query(String key) {
            return Optional.ofNullable(System.getenv(key));
        }
        @Override
        public Traversable<Map.Entry<String, String>> enumerate() {
            return new Traversable<Map.Entry<String, String>>() {
                @Override
                public <M> M fold(M seed, BiFunction<? super M, ? super Map.Entry<String, String>, ? extends M> fn) {
                    for (Map.Entry<String, String> e: System.getenv().entrySet()) {
                        seed = fn.apply(seed, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
                    }
                    return seed;
                }
            };
        }
    },
    EMPTY {
        @Override
        public Optional<String> query(String key) {
            return Optional.empty();
        }
        @Override
        public Traversable<Map.Entry<String, String>> enumerate() {
            return Traversable.empty();
        }
    };

    static final class Composite implements Store {
        final Store head, tail;
        Composite(Store h, Store t) {
            head = h;
            tail = t;
        }
        @Override
        public Optional<String> query(String key) {
            Store o = this;
            do {
                final Composite c = (Composite) o;
                final Optional<String> r = c.head.query(key);
                if (r.isPresent()) return r;
                o = c.tail;
            } while (o instanceof Composite);
            return o.query(key);
        }

        @Override
        public Traversable<Map.Entry<String, String>> enumerate() {
            return new Traversable<>() {
                @Override
                public <M> M fold(M seed, BiFunction<? super M, ? super Map.Entry<String, String>, ? extends M> fn) {
                    class Fold implements BiFunction<M, Map.Entry<String, String>, M> {
                        final Set<String> seen = new HashSet<>();
                        public M apply(M seed, Map.Entry<String, String> entry) {
                            if (seen.add(entry.getKey())) return fn.apply(seed, entry);
                            return seed;
                        }
                    }
                    final Fold fold = new Fold();
                    return tail.enumerate().fold(head.enumerate().fold(seed, fold), fold);
                }
            };
        }
    }

    static Store cons(Store lhs, Store rhs) {
        if (!(lhs instanceof Composite)) return new Composite(lhs, rhs);
        else {
            final Composite cl = (Composite) lhs;
            return new Composite(cl.head, new Composite(cl.tail, rhs));
        }
    }

    static final class VavrMapped implements Store {
        final io.vavr.collection.Map<String, String> map;
        VavrMapped(io.vavr.collection.Map<String, String> map) {
            this.map = map;
        }
        @Override
        public Optional<String> query(String key) {
            return map.get(key).toJavaOptional();
        }

        @Override
        public Traversable<Map.Entry<String, String>> enumerate() {
            return new Traversable<>() {
                @Override
                public <M> M fold(M seed, BiFunction<? super M, ? super Map.Entry<String, String>, ? extends M> fn) {
                    for (Tuple2<String,String> e: map) {
                        seed = fn.apply(seed, new AbstractMap.SimpleImmutableEntry<>(e._1, e._2));
                    }
                    return seed;
                }
            };
        }
    }
}
