package darts.arch.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ByteOrderTest {

    @Test
    public void bigEndian_roundTrips() {
        roundTrip(ByteOrder.BIG_ENDIAN);
    }

    @Test
    public void littleEndian_roundTrips() {
        roundTrip(ByteOrder.LITTLE_ENDIAN);
    }

    private void roundTrip(ByteOrder order) {
        final var buf = OctetString.builder();
        buf.append(0x8877665544332211L, order);
        buf.append(0x44332211, order);
        buf.append((short) 0x2211, order);
        final var str = buf.toOctetString();
        assertEquals(0x8877665544332211L, order.longAt(str, 0));
        assertEquals(0x44332211, order.intAt(str, 8));
        assertEquals((short) 0x2211, order.shortAt(str, 12));
    }
}