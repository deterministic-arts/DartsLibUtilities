package darts.lib.util;

public enum ByteOrder {

    BIG_ENDIAN {
        @Override
        public void put(short value, OctetString.Builder buffer) {
            buffer.append(((int) value) >> 8).append(value);
        }
        @Override
        public void put(int value, OctetString.Builder buffer) {
            buffer.append(value >> 24).append(value >> 16).append(value >> 8).append(value);
        }
        @Override
        public void put(long value, OctetString.Builder buffer) {
            buffer
                .append((int)(value >> 56)).append((int)(value >> 48)).append((int)(value >> 40))
                .append((int)(value >> 32)).append((int)(value >> 24)).append((int)(value >> 16))
                .append((int)(value >> 8)).append((int) value);
        }
        @Override
        public short shortAt(OctetString buffer, int index) {
            return (short) (ldi(buffer, index, 8) | ldi(buffer, index + 1, 0));
        }
        @Override
        public int intAt(OctetString buffer, int index) {
            return ldi(buffer, index, 24) | ldi(buffer, index + 1, 16) | ldi(buffer, index + 2, 8) | ldi(buffer, index + 3, 0);
        }
        @Override
        public long longAt(OctetString buffer, int index) {
            return ldl(buffer, index, 56) | ldl(buffer, index + 1, 48) | ldl(buffer, index + 2, 40)
                | ldl(buffer, index + 3, 32) | ldl(buffer, index + 4, 24) | ldl(buffer, index + 5, 16)
                | ldl(buffer, index + 6, 8) | ldl(buffer, index + 7, 0);
        }
    },

    LITTLE_ENDIAN {
        @Override
        public void put(short value, OctetString.Builder buffer) {
            buffer.append(value).append(((int) value) >> 8);
        }
        @Override
        public void put(int value, OctetString.Builder buffer) {
            buffer.append(value).append(value >> 8).append(value >> 16).append(value >> 24);
        }
        @Override
        public void put(long value, OctetString.Builder buffer) {
            buffer
                .append((int) value).append((int)(value >> 8)).append((int)(value >> 16))
                .append((int)(value >> 24)).append((int)(value >> 32)).append((int)(value >> 40))
                .append((int)(value >> 48)).append((int)(value >> 56));
        }
        @Override
        public short shortAt(OctetString buffer, int index) {
            return (short) (ldi(buffer, index, 0) | ldi(buffer, index + 1, 8));
        }
        @Override
        public int intAt(OctetString buffer, int index) {
            return ldi(buffer, index, 0) | ldi(buffer, index + 1, 8) | ldi(buffer, index + 2, 16) | ldi(buffer, index + 3, 24);
        }
        @Override
        public long longAt(OctetString buffer, int index) {
            return ldl(buffer, index, 0) | ldl(buffer, index + 1, 8) | ldl(buffer, index + 2, 16)
                | ldl(buffer, index + 3, 24) | ldl(buffer, index + 4, 32) | ldl(buffer, index + 5, 40)
                | ldl(buffer, index + 6, 48) | ldl(buffer, index + 7, 56);
        }
    },;

    protected static long ldl(OctetString buffer, int index, int shift) {
        return (((long) buffer.octetAt(index)) & 0xFFL) << shift;
    }

    protected static int ldi(OctetString buffer, int index, int shift) {
        return (buffer.octetAt(index) & 0xFF) << shift;
    }

    public abstract void put(short value, OctetString.Builder buffer);
    public abstract void put(int value, OctetString.Builder buffer);
    public abstract void put(long value, OctetString.Builder buffer);

    public void put(double value, OctetString.Builder buffer) {
        put(Double.doubleToLongBits(value), buffer);
    }

    public void put(float value, OctetString.Builder buffer) {
        put(Float.floatToIntBits(value), buffer);
    }

    public abstract short shortAt(OctetString buffer, int index);
    public abstract int intAt(OctetString buffer, int index);
    public abstract long longAt(OctetString buffer, int index);

    public float floatAt(OctetString buffer, int index) {
        return Float.intBitsToFloat(intAt(buffer, index));
    }

    public double doubleAt(OctetString buffer, int index) {
        return Double.longBitsToDouble(longAt(buffer, index));
    }
}
