package darts.arch.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Basic topological sorting algorithm. Does not handle cyclic graphs
 * but detects those, and raises an exception when appropriate.
 *
 * @param <K>   type of values identifying the nodes
 * @param <N>   type of actual nodes
 */

public final class TopologicalOrder<K, N> {

    private final Function<? super N, ? extends K> keyGetter;
    private final Function<? super N, ? extends Iterable<? extends N>> dependenciesGetter;
    private final BiFunction<? super List<N>, ? super Collection<N>, N> tieBreaker;

    private TopologicalOrder(Function<? super N, ? extends K> keyGetter,
                             Function<? super N, ? extends Iterable<? extends N>> dependenciesGetter,
                             BiFunction<? super List<N>, ? super Collection<N>, N> tieBreaker) {
        this.keyGetter = keyGetter;
        this.dependenciesGetter = dependenciesGetter;
        this.tieBreaker = tieBreaker;
    }

    /**
     * Answers a basic ordering for graphs, whose vertices are
     * instances of {@code N}, and whose edges are detectable via
     * the given function {@code fn}. The resulting order assumes,
     * that it is meaningful to directly compare node instances,
     * i.e., instances of type {@code N} via {@link Object#equals(Object) equals}
     * to determine, whether two objects represent the same
     * conceptual node or not. Also assumes, that the nodes are
     * reasonable hash keys.
     *
     * @param fn    function to enumerate the outgoing edges of a vertex
     * @param <N>   compile-time type of the graph vertices
     *
     * @return  a basic topological ordering
     */

    public static <N> TopologicalOrder<N, N> forEdges(Function<? super N, ? extends Iterable<? extends N>> fn) {
        Objects.requireNonNull(fn);
        return new TopologicalOrder<>(Function.identity(), fn, null);
    }

    public <U> TopologicalOrder<U, N> withIdentity(Function<? super N, ? extends U> fn) {
        Objects.requireNonNull(fn);
        return new TopologicalOrder<>(fn, dependenciesGetter, tieBreaker);
    }

    public TopologicalOrder<K, N> withTieBreaker(BiFunction<? super List<N>, ? super Collection<N>, N> fn) {
        Objects.requireNonNull(fn);
        return new TopologicalOrder<>(keyGetter, dependenciesGetter, fn);
    }

    public static class CircularityException extends IllegalArgumentException {

        private final Collection<?> nodes;

        public CircularityException(Collection<?> nodes) {
            this.nodes = ImmutableList.copyOf(nodes);
        }

        public Collection<?> getNodes() {
            return nodes;
        }
    }

    public List<N> sort(Iterable<? extends N> roots) {
        final State s = new State();
        s.collectAll(roots);
        s.sort();
        return s.result;
    }

    private K keyOf(N node) {
        return keyGetter.apply(node);
    }

    private Iterable<? extends N> dependenciesOf(N node) {
        return dependenciesGetter.apply(node);
    }

    private static final class EntrySetView<N> extends AbstractCollection<N> {
        private final Collection<Entry<N>> underlying;
        EntrySetView(Collection<Entry<N>> underlying) {
            this.underlying = underlying;
        }
        @Override
        public Iterator<N> iterator() {
            return Iterators.unmodifiableIterator(Iterators.transform(underlying.iterator(), e -> e.node));
        }
        @Override
        public int size() {
            return underlying.size();
        }
    }

    private final class State {

        final Map<K, Entry<N>> index = new HashMap<>();
        final Queue<Entry<N>> queue = new LinkedList<>();
        final Set<Entry<N>> unconstrained = new HashSet<>();
        final Set<Entry<N>> constrained = new HashSet<>();
        final List<N> result = new ArrayList<>();

        List<N> resultView = null;
        Collection<N> tieView = null;

        Entry<N> intern(N node) {
            final var id = keyOf(node);
            final var present = index.get(id);
            if (present != null) return present;
            else {
                final var entry = new Entry<>(node);
                index.put(id, entry);
                queue.add(entry);
                return entry;
            }
        }

        void collectAll(Iterable<? extends N> roots) {
            roots.forEach(this::intern);
            while (!queue.isEmpty()) {
                final var entry = queue.remove();
                dependenciesOf(entry.node).forEach(n -> entry.addDependency(intern(n)));
                (entry.isUnconstrained()? unconstrained : constrained).add(entry);
            }
        }

        void release(Entry<N> entry) {
            if (!unconstrained.remove(entry)) {
                throw new IllegalStateException();
            }
            result.add(entry.node);
            if (entry.dependents != null) {
                final Iterator<Entry<N>> iter = entry.dependents.iterator();
                while (iter.hasNext()) {
                    final Entry<N> dep = iter.next();
                    dep.removeDependency(entry);
                    iter.remove();
                    if (dep.isUnconstrained()) {
                        constrained.remove(dep);
                        unconstrained.add(dep);
                    }
                }
            }
        }

        void sort() {
            for (;;) {
                switch (unconstrained.size()) {
                case 0:
                    if (constrained.isEmpty()) return;
                    throw new CircularityException(new EntrySetView<>(constrained));
                case 1:
                    release(unconstrained.iterator().next());
                    continue;
                default:
                    if (tieBreaker == null) release(unconstrained.iterator().next());
                    else {
                        if (resultView == null) resultView = Collections.unmodifiableList(result);
                        if (tieView == null) tieView = new EntrySetView<>(unconstrained);
                        final var pick = tieBreaker.apply(resultView, tieView);
                        release(index.get(keyOf(pick)));
                    }
                    continue;
                }
            }
        }
    }

    private static final class Entry<N> {

        final N node;

        private Set<Entry<N>> dependencies;
        private Set<Entry<N>> dependents;

        Entry(N node) {
            this.node = node;
            this.dependencies = null;
            this.dependents = null;
        }

        @Override
        public String toString() {
            return String.valueOf(node);
        }

        Iterable<Entry<N>> dependents() {
            return dependents == null? Collections.emptyList() : dependents;
        }

        boolean isUnconstrained() {
            return dependencies == null || dependencies.isEmpty();
        }

        void removeDependency(Entry<N> o) {
            dependencies.remove(o);
        }

        void addDependency(Entry<N> o) {
            if (dependencies == null) {
                dependencies = new HashSet<>();
            }
            if (dependencies.add(o)) {
                if (o.dependents == null) {
                    o.dependents = new HashSet<>();
                }
                o.dependents.add(this);
            }
        }
    }
}
