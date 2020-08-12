package darts.arch.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public final class Pair<F,S> implements Serializable {

    private final F first;
    private final S second;

    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    @JsonCreator
    public static <F,S> Pair<F,S> of(@JsonProperty("first") F first, @JsonProperty("second") S second) {
        return new Pair<>(first, second);
    }

    @JsonProperty("first")
    public F first() {
        return first;
    }

    @JsonProperty("second")
    public S second() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        else if (!(o instanceof Pair)) return false;
        else {
            final Pair<?,?> p = (Pair<?,?>) o;
            return Objects.equals(first, p.first) && Objects.equals(second, p.second);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(first) * 31 + Objects.hashCode(second);
    }

    @Override
    public String toString() {
        return String.format("Pair(%s, %s)", first, second);
    }
}
