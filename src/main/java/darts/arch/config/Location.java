package darts.arch.config;

import java.util.Objects;

public final class Location {

    private final String source;
    private final int offset, line;

    public Location(String source, int offset, int line) {
        this.source = Objects.requireNonNull(source);
        this.offset = offset;
        this.line = line;
    }

    public String source() {
        return source;
    }

    public int offset() {
        return offset;
    }

    public int line() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (offset != location.offset) return false;
        if (line != location.line) return false;
        return source.equals(location.source);
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + offset;
        result = 31 * result + line;
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s(%d):%d", source, offset, line);
    }
}
