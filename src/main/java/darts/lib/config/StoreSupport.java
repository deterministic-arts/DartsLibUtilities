package darts.lib.config;

import java.util.Optional;

enum StoreSupport implements Store {

    SYSTEM {
        @Override
        public Optional<String> query(String key) {
            return Optional.ofNullable(System.getProperty(key));
        }
    },
    ENVIRONMENT {
        @Override
        public Optional<String> query(String key) {
            return Optional.ofNullable(System.getenv(key));
        }
    },
    EMPTY {
        @Override
        public Optional<String> query(String key) {
            return Optional.empty();
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
    }
}
