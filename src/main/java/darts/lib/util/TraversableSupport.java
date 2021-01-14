package darts.lib.util;

import java.util.function.*;
import java.util.stream.Collector;

class TraversableSupport {

    static <M,T> Traversable<M> map(Traversable<? extends T> trav, Function<? super T,? extends M> mfn) {
        return new Traversable<M>() {
            @Override
            public <U> U fold(U seed, BiFunction<? super U, ? super M, ? extends U> fn) {
                return trav.fold(seed, (s,e) -> fn.apply(s, mfn.apply(e)));
            }
        };
    }

    static <M,T> Traversable<M> flatMap(Traversable<? extends T> trav, Function<? super T,? extends Traversable<M>> mfn) {
        return new Traversable<M>() {
            @Override
            public <U> U fold(U seed, BiFunction<? super U, ? super M, ? extends U> fn) {
                return trav.fold(seed, (s,e) -> mfn.apply(e).fold(s, fn));
            }
        };
    }

    static <T> Traversable<T> filter(Traversable<? extends T> trav, Predicate<? super T> pred) {
        return new Traversable<T>() {
            @Override
            public <M> M fold(M seed, BiFunction<? super M, ? super T, ? extends M> fn) {
                return trav.fold(seed, (s, e) -> pred.test(e)? fn.apply(s, e) : s);
            }
        };
    }

    static <T> void forEach(Traversable<? extends T> trav, Consumer<? super T> fn) {
        trav.fold(null, (u, e) -> { fn.accept(e); return u; });
    }

    @SuppressWarnings("unchecked")
    static <T, R, A> R collect(Traversable<? extends T> trav, Collector<? super T, A, R> collector) {
        final A container = collector.supplier().get();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        trav.fold(container, (c,e) -> { accumulator.accept(c, e); return c; });
        return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)
            ? (R) container
            : collector.finisher().apply(container);
    }

    static <T> Traversable<T> ofIterable(Iterable<? extends T> it) {
        return new Traversable<T>() {
            @Override
            public <M> M fold(M seed, BiFunction<? super M, ? super T, ? extends M> fn) {
                for (T elt: it) seed = fn.apply(seed, elt);
                return seed;
            }
        };
    }

    static <T> Traversable<T> ofArray(T[] array) {
        return new Traversable<T>() {
            @Override
            public <M> M fold(M seed, BiFunction<? super M, ? super T, ? extends M> fn) {
                for (T elt: array) seed = fn.apply(seed, elt);
                return seed;
            }
        };
    }

    static <T> Traversable<T> ofElement(T elt) {
        return new Traversable<T>() {
            @Override
            public <M> M fold(M seed, BiFunction<? super M, ? super T, ? extends M> fn) {
                return fn.apply(seed, elt);
            }
        };
    }

    static <T> Traversable<T> concat(Traversable<? extends T> t1, Traversable<? extends T> t2) {
        return new Traversable<T>() {
            @Override
            public <M> M fold(M seed, BiFunction<? super M, ? super T, ? extends M> fn) {
                return t2.fold(t1.fold(seed, fn), fn);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T> Traversable<T> empty() {
        return EMPTY;
    }

    private static final Traversable EMPTY = new Traversable() {
        @Override
        public Object fold(Object seed, BiFunction fn) {
            return seed;
        }
    };
}
