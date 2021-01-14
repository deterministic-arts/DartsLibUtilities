package darts.lib.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * An immutable sequence of bytes/octets.
 */

@SuppressWarnings("WeakerAccess")
public final class OctetString implements Serializable, Comparable<OctetString>, Iterable<Integer> {

    private static final long serialVersionUID = 1L;
    private static final OctetString EMPTY = new OctetString(new byte[0]);

    // region Fields and Constructor

    private final byte[] data;
    private transient int hash;

    private OctetString(byte[] data) {
        this.data = data;
        this.hash = 0;
    }

    // endregion

    // region Construction

    public static OctetString empty() {
        return EMPTY;
    }

    public static OctetString of(int... os) {
        if (os.length == 0) return EMPTY;
        else {
            final byte[] buf = new byte[os.length];
            for (int p = 0; p < os.length; ++p) buf[p] = (byte) (0xff & os[p]);
            return new OctetString(buf);
        }
    }

    public static OctetString of(byte... os) {
        return fromByteArray(os);
    }

    public static OctetString fromByteArray(byte[] array, int start, int end) {
        if (start < 0 || end < start || array.length < end) throw new IndexOutOfBoundsException();
        else if (end == start) return EMPTY;
        else {
            final byte[] copy = new byte[end - start];
            System.arraycopy(array, start, copy, 0, end - start);
            return new OctetString(copy);
        }
    }

    public static OctetString fromByteArray(byte[] array, int start) {
        return fromByteArray(array, start, array.length);
    }

    public static OctetString fromByteArray(byte[] array) {
        return array.length == 0? EMPTY : new OctetString(array.clone());
    }

    @JsonCreator
    public static OctetString fromString(String s) {
        final byte[] arr = Base64.getUrlDecoder().decode(s);
        return arr.length == 0? EMPTY : new OctetString(arr);
    }

    private static final SecureRandom prng = new SecureRandom();

    public static OctetString randomString(int len) {
        if (len < 0) throw new IllegalArgumentException();
        else if (len == 0) return EMPTY;
        else {
            final byte[] buf = new byte[len];
            prng.nextBytes(buf);
            return new OctetString(buf);
        }
    }

    // endregion

    // region Sub-String Comparisons

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean startsWith(OctetString os) {
        final int olen = os.data.length;
        return data.length >= olen && Arrays.equals(data, 0, olen, os.data, 0, olen);
    }

    public boolean endsWith(OctetString os) {
        final int olen = os.data.length;
        return data.length >= olen && Arrays.equals(data, data.length - olen, olen, os.data, 0, olen);
    }

    // endregion

    // region Accessing Content

    public int octetAt(int index) {
        return data[index] & 0xff;
    }

    public int length() {
        return data.length;
    }

    public boolean isEmpty() {
        return data.length == 0;
    }

    public OctetString substring(int start, int end) {
        if (start == 0 && end == data.length) return this;
        return fromByteArray(data, start, end);
    }

    public OctetString substring(int start) {
        return substring(start, data.length);
    }

    public byte[] toByteArray() {
        return data.clone();
    }

    public void getBytes(int srcStart, int srcEnd, byte[] buffer, int dstStart) {
        if (srcStart < 0 || srcEnd < srcStart || data.length < srcEnd) {
            throw new IndexOutOfBoundsException();
        } else {
            final int len = srcEnd - srcStart;
            if (dstStart < 0 || buffer.length - len < dstStart) throw new IndexOutOfBoundsException();
            else {
                System.arraycopy(data, srcStart, buffer, dstStart, len);
            }
        }
    }

    public ByteBuffer toByteBuffer(boolean readOnly) {
        return readOnly? ByteBuffer.wrap(data).asReadOnlyBuffer() : ByteBuffer.wrap(data.clone());
    }

    public ByteBuffer toByteBuffer() {
        return toByteBuffer(true);
    }

    @Override
    @JsonValue
    public String toString() {
        return Base64.getUrlEncoder().encodeToString(data);
    }

    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveIterator.OfInt() {
            private int ptr = 0;
            @Override
            public int nextInt() {
                if (ptr >= data.length) throw new NoSuchElementException();
                return data[ptr++];
            }
            @Override
            public boolean hasNext() {
                return ptr < data.length;
            }
        };
    }

    // endregion

    // region Hashing and Comparing

    @Override
    public int compareTo(OctetString o) {
        final byte[] lb = data;
        final byte[] rb = o.data;
        final int llen = lb.length;
        final int rlen = rb.length;
        final int mlen = Math.min(llen, rlen);
        for (int p = 0; p < mlen; ++p) {
            final int le = 0xff & lb[p];
            final int re = 0xff & rb[p];
            final int diff = le - re;
            if (diff != 0) return diff;
        }
        return Integer.compare(llen, rlen);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        else if (!(o instanceof OctetString)) return false;
        else return Arrays.equals(data, ((OctetString) o).data);
    }

    @Override
    public int hashCode() {
        final int h = hash;
        if (h != 0) return h;
        else {
            final int h1 = Arrays.hashCode(data);
            final int h2 = h1 == 0? 1 : h1;
            return hash = h2;
        }
    }

    // endregion

    // region Serialization

    private static class Proxy implements Serializable {

        private static final long serialVersionUID = 1L;
        private byte[] data;

        Proxy(OctetString s) {
            data = s.data;
        }

        private Object readResolve() {
            return data.length == 0? EMPTY : new OctetString(data);
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeInt(data.length);
            stream.write(data);
        }

        private void readObject(ObjectInputStream stream) throws IOException {
            final int len = stream.readInt();
            if (len < 0) throw new InvalidObjectException("octet string broke in hibernation");
            else {
                final var buffer = new byte[len];
                stream.readFully(buffer);
                data = buffer;
            }
        }
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    // endregion

    // region Builder

    public Builder toBuilder() {
        return new Builder(length()).append(data, 0, data.length);
    }

    public static Builder builder() {
        return new Builder(16);
    }

    public static final class Builder {

        private byte[] buffer;
        private int length;

        Builder(int capacity) {
            buffer = new byte[Math.max(capacity, 16)];
            length = 0;
        }

        public String toText(Charset cs) {
            return new String(buffer, 0, length, cs);
        }

        public Builder clear() {
            length = 0;
            return this;
        }

        public OctetString toOctetString() {
            return length == 0? empty() : new OctetString(Arrays.copyOf(buffer, length));
        }

        public int length() {
            return length;
        }

        public Builder append(int octet) {
            ensureRoom(1);
            buffer[length++] = (byte) (0xFF & octet);
            return this;
        }

        public Builder append(short value, ByteOrder order) {
            order.put(value, this);
            return this;
        }

        public Builder append(int value, ByteOrder order) {
            order.put(value, this);
            return this;
        }

        public Builder append(long value, ByteOrder order) {
            order.put(value, this);
            return this;
        }

        public Builder append(float value, ByteOrder order) {
            order.put(value, this);
            return this;
        }

        public Builder append(double value, ByteOrder order) {
            order.put(value, this);
            return this;
        }

        public Builder append(OctetString buf) {
            return append(buf, 0, buf.length());
        }

        public Builder append(OctetString buf, int start) {
            return append(buf, start, buf.length());
        }

        public Builder append(OctetString buf, int start, int end) {
            if (start < 0 || end < start || buf.length() < end) throw new IndexOutOfBoundsException();
            else {
                final int len = end - start;
                ensureRoom(len);
                System.arraycopy(buf.data, start, buffer, length, len);
                length += len;
                return this;
            }
        }

        public Builder append(byte[] buffer) {
            return append(buffer, 0, buffer.length);
        }

        public Builder append(byte[] buffer, int start) {
            return append(buffer, start, buffer.length);
        }

        public Builder append(byte[] buf, int start, int end) {
            if (end < start || start < 0 || buf.length < end) throw new IndexOutOfBoundsException();
            else {
                final int len = end - start;
                ensureRoom(len);
                System.arraycopy(buf, start, buffer, length, len);
                length += len;
                return this;
            }
        }

        private void ensureRoom(int required) {
            if (length + required > buffer.length) {
                final int missing = required + length - buffer.length;
                final int defaultGrowth = buffer.length + (buffer.length >>> 1);
                final int newLen = Math.max(defaultGrowth, buffer.length + missing);
                buffer = Arrays.copyOf(buffer, newLen);
            }
        }
    }

    // endregion
}
