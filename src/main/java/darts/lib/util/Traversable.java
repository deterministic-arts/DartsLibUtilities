package darts.lib.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * A traversable is basically a collection of elements, that
 * can be enumerated. The interface fills a similar niche as Java's
 * standard {@link java.util.stream.Stream Stream} or {@link Iterable}.
 * The most important difference between a traversable and a stream
 * or iterable is, that the traversable implementation remains in
 * control of the resources needed to perform the enumeration. In
 * particular, it can always make sure, that resources are cleaned
 * up properly after the enumeration process has been finished.
 *
 * <p>Though Java's {@linkplain java.util.stream.Stream streams}
 * are {@linkplain AutoCloseable}, a lot of uses of the stream API
 * allow you to forget that fact. This cannot happen with this
 * interface, making it better suited, say, to provide easy
 * iteration over database result sets, etc.
 *
 * <p>Like standard streams, traversables distinguish between
 * terminal operations, that cause the elements to be actually
 * produced, and non-terminal operations (transformations), which
 * compose new traversables from existing ones without actually
 * generating any elements. Unlike streams, traversables can be used
 * to enumerate their elements as many times as needed. In particular,
 * every invocation of {@link #fold(Object, BiFunction)} is supposed
 * to produce the elements fresh from whatever underlying store
 * they have to be harvested.
 *
 * @param <T>   type of the elements produced
 */

public interface Traversable<T> {

    /**
     * Applies the given function to each element of this collection
     * in proper encounter order, allowing the function to accumulate
     * a result.
     *
     * <p>This method may be called any number of times. Whenever called,
     * the expectation is, that the implementation reevaluates its
     * internal structures, providing a fresh view of the underlying
     * data (e.g., making a new query into the database, scanning the
     * file system, etc.) If the underlying collection is empty, the
     * result is the given seed value, otherwise, it's the last accumulator
     * value returned by {@code fn}.
     *
     * @param seed  initial accumulator value
     * @param fn    computation to invoke for all elements
     * @param <M>   type of the accumulator value and result
     *
     * @return  whatever the final accumulator value is
     */

    <M> M fold(M seed, BiFunction<? super M, ? super T, ? extends M> fn);

    // region Terminal Operations

    default void forEach(Consumer<? super T> fn) {
        TraversableSupport.forEach(this, fn);
    }

    default <R, A> R collect(Collector<? super T, A, R> collector) {
        return TraversableSupport.collect(this, collector);
    }

    // endregion

    // region Transformations

    default <U> Traversable<U> map(Function<? super T, ? extends U> fn) {
        return TraversableSupport.map(this, fn);
    }

    default <U> Traversable<U> flatMap(Function<? super T, ? extends Traversable<U>> fn) {
        return TraversableSupport.flatMap(this, fn);
    }

    default Traversable<T> filter(Predicate<? super T> fn) {
        return TraversableSupport.filter(this,  fn);
    }

    // endregion

    // region Construction

    static <T> Traversable<T> ofAll(Iterable<? extends T> it) {
        return TraversableSupport.ofIterable(it);
    }

    static <T> Traversable<T> ofAll(T[] it) {
        return TraversableSupport.ofArray(it);
    }

    static <T> Traversable<T> empty() {
        return TraversableSupport.empty();
    }

    static <T> Traversable<T> of(T elt1) {
        return TraversableSupport.ofElement(elt1);
    }

    @SafeVarargs
    static <T> Traversable<T> of(T elt1, T elt2, T... rest) {
        return rest.length == 0? concat(of(elt1), of(elt2)) : concat(of(elt1), concat(of(elt2), TraversableSupport.ofArray(rest)));
    }

    static <T> Traversable<T> concat(Traversable<? extends T> t1, Traversable<? extends T> t2) {
        return TraversableSupport.concat(t1, t2);
    }

    // endregion
}
