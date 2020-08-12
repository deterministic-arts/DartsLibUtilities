package darts.arch.config;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class Parser {

    // region API

    public static Map<String, String> parse(String source, Reader reader, Map<String, String> defaults) throws IOException {
        return new Parser(source, reader).parseBody(defaults);
    }

    public static Map<String, String> parse(String source, Reader reader) throws IOException {
        return new Parser(source, reader).parseBody();
    }

    public static Map<String, String> parse(File file) throws IOException {
        try (final InputStream stream = new FileInputStream(file);
             final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return parse(file.toString(), reader);
        }
    }

    public static Map<String, String> parse(URI file) throws IOException {
        return parse(file.toURL());
    }

    public static Map<String, String> parse(URL file) throws IOException {

        final URLConnection cnx = file.openConnection();

        cnx.setDoInput(true);
        cnx.setDoOutput(false);
        cnx.setAllowUserInteraction(false);

        try (final InputStream stream = cnx.getInputStream();
             final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

            return parse(file.toExternalForm(), reader);
        }
    }

    // endregion

    // region Parser

    private final Lexer lexer;

    private Parser(String source, Reader reader) {
        lexer = new Lexer(source, reader);
    }

    private String join(String prefix, String name) {
        if (prefix.isEmpty()) return name;
        return prefix.concat(".").concat(name);
    }

    private String parseKey(String prefix) throws IOException {
        if (lexer.token != WORD) throw new AssertionError();
        else {
            final StringBuilder sb = new StringBuilder(prefix);
            if (sb.length() > 0) sb.append('.');
            sb.append(lexer.value);
            lexer.advance();
            while (lexer.token == DOT) {
                lexer.advance();
                if (lexer.token != WORD) {
                    throw new SyntaxException(lexer.location(), "unexpected token; a word was required here");
                } else {
                    sb.append('.').append(lexer.value);
                    lexer.advance();
                }
            }
            return sb.toString();
        }
    }

    private Map<String, String> parseBody(Map<String, String> init) throws IOException {
        lexer.advance();
        final Map<String, String> map = parseBody("", init);
        if (lexer.token != END) throw new SyntaxException(lexer.position(), "found data after settings");
        return map;
    }

    private Map<String, String> parseBody() throws IOException {
        return parseBody(HashMap.empty());
    }

    private Map<String, String> parseBody(String prefix, Map<String, String> map) throws IOException {
        while (lexer.token == WORD) {
            map = parseEntry(prefix, map);
        }
        return map;
    }

    private Map<String, String> parseEntry(String prefix, Map<String, String> map) throws IOException {

        if (lexer.token == WORD) {

            final String key = parseKey(prefix);

            switch (lexer.token) {

                case EQUAL:
                    lexer.advance();
                    if (lexer.token == WORD) {
                        final String value = parseKey("");
                        return map.put(key, value);
                    }
                    throw new SyntaxException(lexer.location(), "unexpected token");

                case OPEN:
                    lexer.advance();
                    map = parseBody(key, map);
                    if (lexer.token != CLOSE) {
                        throw new SyntaxException(lexer.location(), "unexpected token");
                    } else {
                        lexer.advance();
                        return map;
                    }
            }
        }

        throw new SyntaxException(lexer.location(), "unexpected token");
    }

    // endregion

    // region Lexer

    private static final int END = 0;
    private static final int OPEN = 1;
    private static final int CLOSE = 2;
    private static final int DOT = 3;
    private static final int EQUAL = 4;
    private static final int WORD = 5;

    private static final class Lexer {

        final StringBuilder builder;
        final String source;
        final Reader reader;

        boolean eof;
        int buffer;
        int cOffset, cLine;
        Location cLocation;

        int token;
        String value;
        int tOffset, tLine;
        Location tLocation;

        Lexer(String src, Reader rd) {
            builder = new StringBuilder();
            reader = rd;
            source = src;
            buffer = -1;
            cOffset = 0;
            cLine = 1;
            tOffset = 0;
            tLine = 1;
            token = -1;
            value = "";
            eof = false;
        }

        private String finish() {
            final String r = builder.toString();
            builder.setLength(0);
            return r;
        }

        private void mark() {
            tOffset = cOffset;
            tLine = cLine;
            tLocation = null;
        }

        private void set(int t) {
            token = t;
            value = "";
        }

        private void set(int t, String s) {
            token = t;
            value = s;
        }

        private boolean isEndOfComment(int ch) {
            return ch == -1 || ch == 10;
        }

        private boolean isNameStart(int ch) {
            return 'a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z' || ch == '_';
        }

        private boolean isNameChar(int ch) {
            return isNameStart(ch) || '0' <= ch && ch <= '9';
        }

        private void parseNumber() throws IOException {
            final int BEFORE_SIGN = 0, AFTER_SIGN = 1, INTEGER = 2;
            final int AFTER_DOT = 3, FRACTION = 4, AFTER_EXP_MARKER = 5;
            final int AFTER_EXP_SIGN = 6, EXPONENT = 7;
            int state = BEFORE_SIGN;
            loop: for (;;) {
                final int ch = peek();
                switch (ch) {
                default: break loop;
                case '+': case '-':
                    switch (state) {
                    case BEFORE_SIGN:
                        if (ch != '+') builder.append((char) read());
                        else read();
                        state = AFTER_SIGN;
                        continue loop;
                    case AFTER_EXP_MARKER:
                        builder.append((char) read());
                        state = AFTER_EXP_SIGN;
                        continue loop;
                    }
                    break loop;
                case '.':
                    switch (state) {
                    case INTEGER:
                        builder.append((char) read());
                        state = AFTER_DOT;
                        continue loop;
                    }
                    break loop;
                case 'e': case 'E':
                    switch (state) {
                    case INTEGER:
                        builder.append(".e");
                        read();
                        state = AFTER_EXP_MARKER;
                        continue loop;
                    case FRACTION:
                        builder.append((char) read());
                        state = AFTER_EXP_MARKER;
                        continue loop;
                    }
                    break loop;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    switch (state) {
                    case AFTER_SIGN: case BEFORE_SIGN:
                        state = INTEGER; // Fall Through
                    case INTEGER:
                        builder.append((char) read());
                        continue loop;
                    case AFTER_DOT:
                        state = FRACTION;   // Fall Through
                    case FRACTION:
                        builder.append((char) read());
                        continue loop;
                    case AFTER_EXP_MARKER: case AFTER_EXP_SIGN:
                        state = EXPONENT;   // Fall Through
                    case EXPONENT:
                        builder.append((char) read());
                        continue loop;
                    }
                    break loop;
                }
            }
            switch (state) {
            case INTEGER: case FRACTION: case EXPONENT:
                set(WORD, finish());
                return;
            }
            throw new SyntaxException(location(), "unsupported number syntax");
        }

        private void parseLiteral() throws IOException {

            for (;;) {

                final int ch = read();

                switch (ch) {
                    case -1:
                        throw new SyntaxException(location(), "unterminated literal");
                    case '"':
                        set(WORD, finish());
                        return;
                    case '\\':
                        switch (peek()) {
                            case -1:
                                throw new SyntaxException(location(), "unterminated literal");
                            case 'b':
                                read();
                                builder.append('\b');
                                continue;
                            case 't':
                                read();
                                builder.append('\t');
                                continue;
                            case 'n':
                                read();
                                builder.append('\n');
                                continue;
                            case 'r':
                                read();
                                builder.append('\r');
                                continue;
                            case 'u':
                                throw new UnsupportedOperationException("not yet");
                            case '"':
                                read();
                                builder.append('"');
                                continue;
                            case '\\':
                                read();
                                builder.append('\\');
                                continue;
                            default:
                                throw new SyntaxException(position(), "unsupported escape command");
                        }
                    default:
                        builder.append((char) ch);
                        continue;
                }
            }
        }

        Location position() {
            if (cLocation == null) cLocation = new Location(source, cOffset, cLine);
            return cLocation;
        }

        Location location() {
            if (tLocation == null) tLocation = new Location(source, tOffset, tLine);
            return tLocation;
        }

        void advance() throws IOException {
            for (;;) {
                mark();
                final int ch = peek();
                switch (ch) {
                    case -1:
                        set(END);
                        return;
                    case 9: case 10: case 13: case 32:
                        read();
                        continue;
                    case '#':
                        while (!isEndOfComment(read())) /**/;
                        continue;
                    case '.':
                        read();
                        set(DOT);
                        return;
                    case '{':
                        read();
                        set(OPEN);
                        return;
                    case '}':
                        read();
                        set(CLOSE);
                        return;
                    case '=':
                        read();
                        set(EQUAL);
                        return;
                    case '"':
                        read();
                        parseLiteral();
                        return;
                    case '+': case '-':
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        parseNumber();
                        return;
                    default:
                        if (!isNameStart(ch)) {
                            throw new SyntaxException(position(), "unexpected character '%c'", (char) ch);
                        } else {
                            do {
                                builder.append((char) read());
                            } while (isNameChar(peek()));
                            set(WORD, finish());
                            return;
                        }
                }
            }
        }

        private int peek() throws IOException {
            if (buffer >= 0) return buffer;
            else if (eof) return -1;
            else {
                buffer = Math.max(-1, reader.read());
                eof = buffer == -1;
                return buffer;
            }
        }

        private int read() throws IOException {
            final int ch;
            if (buffer >= 0) {
                ch = buffer;
                buffer = -1;
            } else {
                if (eof) ch = -1;
                else {
                    ch = Math.max(-1, reader.read());
                    eof = ch == -1;
                }
            }
            switch (ch) {
                case -1:
                    return ch;
                case 10:
                    cLocation = null;
                    cLine += 1;
                    cOffset += 1;
                    return ch;
                default:
                    cLocation = null;
                    cOffset += 1;
                    return ch;
            }
        }
    }

    // endregion
}
