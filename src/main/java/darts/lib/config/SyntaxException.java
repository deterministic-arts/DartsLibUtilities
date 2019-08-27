package darts.lib.config;

import java.io.IOException;

public class SyntaxException extends IOException {

    private Location location;

    public SyntaxException(Location location, String control, Object... args) {
        super(location.toString() + ": " + String.format(control, args));
    }

    public Location getLocation() {
        return location;
    }
}
