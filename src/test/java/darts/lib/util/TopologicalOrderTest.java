package darts.lib.util;

import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertTrue;

public class TopologicalOrderTest {

    static final TopologicalOrder<String, Node> simpleOrder =
        TopologicalOrder.<Node>forEdges(e -> e.dependencies).withIdentity(e -> e.key);

    @Test(expected = TopologicalOrder.CircularityException.class)
    public void detects_circularity() {
        final Graph G = new Graph();
        G.node("a", "b").node("b", "c").node("c", "a");
        simpleOrder.sort(G.pick("a"));
    }

    @Test
    public void sorts_properly() {

        final Graph G = new Graph();
        G.node("app", "main.o", "lib.o")
            .node("main.o", "main.c", "lib.h")
            .node("lib.o", "lib.c", "lib.h");

        final List<Node> result = simpleOrder.sort(G.pick("app"));
        assertConstraints(result,
            "app", "lib.o",
            "app", "main.o",
            "main.o", "main.c",
            "main.o", "lib.h",
            "lib.o", "lib.c",
            "lib.o", "lib.h");
    }

    @Test
    public void does_tie_breaking() {

        final Graph G = new Graph();
        G.node("a", "b1", "b2");

        final List<Node> result = simpleOrder
            .withTieBreaker((unused, nodes) -> Traversable.ofAll(nodes).fold(null, (best, n) ->
                best == null || n.key.compareTo(best.key) < 0? n : best
            ))
            .sort(G.pick("a"));

        // Without the tie breaker, both, [b1, b2, a] and [b2, b1, a] would be
        // admissible here

        assertConstraints(result,
            "a", "b1",
            "a", "b2",
            "b2", "b1"
        );
    }

    private void assertConstraints(List<Node> nodes, String... constraints) {
        final var idx = new HashMap<String,Integer>();
        nodes.forEach(n -> idx.put(n.key, idx.size()));
        System.out.println(nodes);
        System.out.println(idx);
        for (int p = 0; p < constraints.length; ) {
            final String after = constraints[p++];
            final String before = constraints[p++];
            final var bp = idx.get(before);
            final var ap = idx.get(after);
            assertTrue(before + " vs " + after, bp != null && ap != null && bp < ap);
        }
    }

    static final class Node {
        final String key;
        final Set<Node> dependencies;
        Node(String key) {
            this.key = key;
            this.dependencies = new HashSet<>();
        }
        @Override
        public String toString() {
            return String.format("%s(%s)", key, dependencies.stream().map(e -> e.key).collect(joining(", ")));
        }
    }

    static final class Graph {
        final Map<String,Node> index = new HashMap<>();
        private Node make(String name) {
            return index.computeIfAbsent(name, Node::new);
        }
        Graph node(String name, String... dependencies) {
            final var n = make(name);
            Arrays.stream(dependencies).map(this::make).forEach(n.dependencies::add);
            return this;
        }
        Collection<Node> pick(String... names) {
            return Arrays.stream(names).map(index::get).collect(Collectors.toList());
        }
    }
}