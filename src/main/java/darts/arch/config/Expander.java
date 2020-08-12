package darts.arch.config;

import darts.arch.util.Pair;

import java.util.NoSuchElementException;

class Expander {

    interface Resolver {
        Pair<String,Resolver> resolve(String subst);
    }

    public static String up(String key) {
        final int dot = key.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException();
        return key.substring(0, dot);
    }

    static Resolver forStore(Store store, String baseName) {
        return subst -> {
            if (!subst.startsWith(".")) {
                return Pair.of(store.get(subst), forStore(store, subst));
            } else {
                int len = baseName.length();
                int p = 0;
                while (p < subst.length() && subst.charAt(p) == '.') {
                    if (len == 0) throw new NoSuchElementException(subst);
                    else {
                        final int dot = baseName.lastIndexOf('.', len - 1);
                        if (dot < 0) len = 0;
                        else len = dot;
                        ++p;
                    }
                }
                if (p == subst.length()) throw new IllegalArgumentException("invalid substitution key: " + subst);
                else {
                    final String eff = len == 0? subst.substring(p) : baseName.substring(0, len) + "." + subst.substring(p);
                    return Pair.of(store.get(eff), forStore(store, eff));
                }
            }
        };
    }

    public static String expand(String template, Resolver resolver) {

        final StringBuilder buffer = new StringBuilder(template.length());

        Lexer lexer = new Lexer(resolver, template, null);

        do {

            inner: for (;;) {

                lexer.advance();

                switch (lexer.token) {
                    case END:
                        break inner;
                    case TEXT:
                        buffer.append(lexer.value);
                        continue inner;
                    case SUBST:
                        if (lexer.depth == 16) throw new IllegalArgumentException("substitution depth limit reached");
                        final Pair<String,Resolver> rp = lexer.resolver.resolve(lexer.value);
                        lexer = lexer.push(rp.first(), rp.second());
                        continue inner;
                }

                throw new AssertionError();
            }

            lexer = lexer.parent;

        } while (lexer != null);

        return buffer.toString();
    }

    static final int END = 0;
    static final int TEXT = 1;
    static final int SUBST = 2;

    static class Lexer {

        final Resolver resolver;
        final Lexer parent;
        final String text;
        final int length;
        final int depth;
        int position;

        int token;
        String value;

        Lexer(Resolver n, String s, Lexer p) {
            depth = p == null? 1 : 1 + p.depth;
            parent = p;
            resolver = n;
            text = s;
            length = s.length();
            position = 0;
            token = -1;
            value = "";
        }

        Lexer push(String subst, Resolver resolver) {
            return new Lexer(resolver, subst, this);
        }

        private void consumeSubst() {
            ++position; // Skip initial "$"
            if (position >= length) {
                throw new IllegalArgumentException("malformed template: " + text);
            } else {
                switch (text.charAt(position)) {
                    case '$':
                        consumeText();
                        return;
                    case '{':
                        final int openLoc = ++position;
                        found:
                        {
                            while (position < length) {
                                if (text.charAt(position) == '}') break found;
                                ++position;
                            }
                            throw new IllegalArgumentException("malformed template: " + text);
                        }
                        value = text.substring(openLoc, position++);
                        token = SUBST;
                        return;
                    default:
                        throw new IllegalArgumentException("malformed template: " + text);
                }
            }
        }

        private void consumeText() {

            final int start = position;

            while (position < length) {

                if (text.charAt(position) != '$') {

                    ++position;

                } else {

                    if (position + 1 >= length || text.charAt(position + 1) != '$') {

                        value = text.substring(start, position);
                        token = TEXT;
                        return;

                    } else {

                        value = text.substring(start, position + 1);
                        token = TEXT;
                        position += 2;
                        return;
                    }
                }
            }

            value = text.substring(start, position);
            token = TEXT;
        }

        void advance() {
            if (position >= length) {
                value = "";
                token = END;
            } else {
                switch (text.charAt(position)) {
                    case '$':
                        consumeSubst();
                        break;
                    default:
                        consumeText();
                        break;
                }
            }
        }
    }
}
