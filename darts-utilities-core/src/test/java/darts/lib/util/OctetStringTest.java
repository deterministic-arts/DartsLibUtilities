package darts.lib.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class OctetStringTest {

    // region Ordering, Equality, Hashing

    @Test
    public void compareTo_based_on_unsigned_values() {
        final var b1 = OctetString.fromByteArray(new byte[] { -1 });
        final var b2 = OctetString.fromByteArray(new byte[] { 0 });
        assertTrue(b1.compareTo(b2) > 0);
        assertTrue(b2.compareTo(b1) < 0);
    }

    @Test
    public void compareTo_compatible_with_equals() {
        final var data = new OctetString[] {
            OctetString.empty(),
            OctetString.randomString(1),
            OctetString.randomString(11),
            OctetString.randomString(111)
        };
        for (var o: data) {
            for (var i: data) {
                if (o.equals(i)) {
                    assertEquals(0, o.compareTo(i));
                    assertEquals(0, i.compareTo(o));
                } else {
                    assertTrue(o.compareTo(i) != 0);
                    assertTrue(i.compareTo(o) != 0);
                }
            }
        }
    }

    @Test
    public void compareTo_is_total_ordering() {
        final var data = new OctetString[] {
            OctetString.empty(),
            OctetString.randomString(1),
            OctetString.randomString(11),
            OctetString.randomString(111)
        };
        for (var o: data) {
            for (var i: data) {
                final var so = isign(o.compareTo(i));
                final var si = isign(i.compareTo(o));
                assertEquals(-so, si);
                if (so == 0) {
                    assertEquals(so, si);
                }
            }
        }
    }

    @Test
    public void equals_is_based_on_the_contents() {
        final var data1 = new OctetString[] {
            OctetString.empty(),
            OctetString.randomString(1),
            OctetString.randomString(11),
            OctetString.randomString(111)
        };
        final var data2 = Arrays.stream(data1)
            .map(OctetString::toByteArray)
            .map(OctetString::fromByteArray)
            .toArray(OctetString[]::new);
        for (var o: data1) {
            for (var i: data2) {
                if (o.equals(i)) {
                    assertArrayEquals(o.toByteArray(), i.toByteArray());
                } else {
                    assertFalse(Arrays.equals(o.toByteArray(), i.toByteArray()));
                }
            }
        }
    }

    @Test
    public void hashCode_is_compatible_with_equals() {
        final var data1 = new OctetString[] {
            OctetString.empty(),
            OctetString.randomString(1),
            OctetString.randomString(11),
            OctetString.randomString(111)
        };
        final var data2 = Arrays.stream(data1)
            .map(OctetString::toByteArray)
            .map(OctetString::fromByteArray)
            .toArray(OctetString[]::new);
        for (var o: data1) {
            for (var i: data2) {
                if (o.equals(i)) {
                    assertEquals(o.hashCode(), i.hashCode());
                }
            }
        }
    }

    // endregion

    // region ToString, FromString

    @Test
    public void toString_round_trips() {
        final int nIterations = 100;
        final var rng = new Random();
        final int maxLen = 1024;
        assertEquals(OctetString.empty(), OctetString.fromString(OctetString.empty().toString()));
        for (int p = 0; p < nIterations; ++p) {
            final var expected = OctetString.randomString(rng.nextInt(maxLen));
            final var actual = OctetString.fromString(expected.toString());
            Assert.assertEquals(expected, actual);
        }
    }

    // endregion

    // region Builder

    @Test
    public void builder_initially_empty() {
        assertEquals(OctetString.empty(), OctetString.builder().toOctetString());
    }

    @Test
    public void builder_grows_as_needed_when_filled_bytewise() {
        final var bb = OctetString.builder();
        for (int p = 0; p < 2048; ++p) {
            bb.append(p);
        }
        final var rs = bb.toOctetString();
        assertEquals(2048, rs.length());
        for (int p = 0; p < 2048; ++p) {
            assertEquals(p & 0xFF, rs.octetAt(p));
        }
    }

    @Test
    public void builder_grows_as_needed_when_filled_in_chunks() {
        final var rng = new Random();
        final byte[][] bufs = { new byte[133], new byte[76], new byte[279], new byte[3147] };
        for (var b: bufs) rng.nextBytes(b);
        final var bb = OctetString.builder();
        for (var b: bufs) {
            bb.append(b);
        }
        final var rs = bb.toOctetString();
        int offs = 0;
        for (var b: bufs) {
            assertEquals(OctetString.fromByteArray(b), rs.substring(offs, offs + b.length));
            offs += b.length;
        }
        assertEquals(offs, rs.length());
    }

    // endregion

    // region Serialization

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        assertEquals(OctetString.empty(), rountrip(OctetString.empty()));
        assertEquals(OctetString.of(1, 2, 3, 4, 5), rountrip(OctetString.of(1, 2, 3, 4, 5)));
    }

    private OctetString rountrip(OctetString object) throws IOException, ClassNotFoundException {
        final var ob = new ByteArrayOutputStream();
        final var os = new ObjectOutputStream(ob);
        os.writeObject(object);
        os.close();
        ob.close();
        final var ib = new ByteArrayInputStream(ob.toByteArray());
        final var is = new ObjectInputStream(ib);
        final var result = (OctetString) is.readObject();
        is.close();
        ib.close();
        return result;
    }

    // endregion

    // region Helpers

    private static int isign(int s) {
        return s < 0? -1 : (s > 0? 1 : 0);
    }

    // endregion
}