package darts.arch.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TraversableTest {

    @Test
    public void fromIterable_empty() {

        final var sub = Traversable.ofAll(ImmutableList.of());
        assertEquals(0, sub.fold(0, (c, e) -> 1 + c).intValue());
    }

    @Test
    public void fromIterable_nonEmpty() {
        {
            final var sub = Traversable.ofAll(ImmutableList.of(1));
            assertEquals(1, sub.fold(0, (c, e) -> 1 + c).intValue());
        }
        {
            final var sub = Traversable.ofAll(ImmutableList.of(1, 2, 3));
            assertEquals(3, sub.fold(0, (c, e) -> 1 + c).intValue());
        }
    }

    @Test
    public void derived_collect_empty() {
        final ImmutableList<Integer> input = ImmutableList.of();
        final var actual = Traversable.ofAll(input).collect(Collectors.toList());
        assertEquals(input, actual);
    }

    @Test
    public void derived_collect_nonEmpty() {
        final ImmutableList<Integer> input = ImmutableList.of(0, 1, 2, 3, 4);
        final var actual = Traversable.ofAll(input).collect(Collectors.toList());
        assertEquals(input, actual);
    }

    @Test
    public void derived_filter_empty() {
        final ImmutableList<Integer> input = ImmutableList.of();
        final var actual = Traversable.ofAll(input).filter(n -> (n & 1) == 0).collect(Collectors.toList());
        assertEquals(ImmutableList.of(), actual);
    }

    @Test
    public void derived_filter_nonEmpty() {
        final ImmutableList<Integer> input = ImmutableList.of(0, 1, 2, 3, 4);
        final var actual = Traversable.ofAll(input).filter(n -> (n & 1) == 0).collect(Collectors.toList());
        assertEquals(ImmutableList.of(0, 2, 4), actual);
    }

    @Test
    public void derived_map_empty() {
        final ImmutableList<Integer> input = ImmutableList.of();
        final var actual = Traversable.ofAll(input).map(String::valueOf).collect(Collectors.toList());
        assertEquals(ImmutableList.of(), actual);
    }

    @Test
    public void derived_map_nonEmpty() {
        final ImmutableList<Integer> input = ImmutableList.of(0, 1, 2, 3, 4);
        final var actual = Traversable.ofAll(input).map(String::valueOf).collect(Collectors.toList());
        assertEquals(ImmutableList.of("0", "1", "2", "3", "4"), actual);
    }

    @Test
    public void derived_flatMap_empty() {
        final ImmutableList<ImmutableList<Integer>> input = ImmutableList.of();
        final var actual = Traversable.ofAll(input).flatMap(Traversable::ofAll).collect(Collectors.toList());
        assertEquals(ImmutableList.of(), actual);
    }

    @Test
    public void derived_flatMap_nonEmpty() {
        final ImmutableList<ImmutableList<Integer>> input = ImmutableList.of(ImmutableList.of(1), ImmutableList.of(2, 3), ImmutableList.of(), ImmutableList.of(4));
        final var actual = Traversable.ofAll(input).flatMap(Traversable::ofAll).collect(Collectors.toList());
        assertEquals(ImmutableList.of(1, 2, 3, 4), actual);
    }
}