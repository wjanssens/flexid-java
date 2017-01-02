import org.testng.annotations.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

public class FlexIdTest {

    public static final long INSTAGRAM_EPOCH = 1293840000000L;

    @Test
    public void testMillis() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 8, 8);
        long id = g.generate(0x5A5A5A5A5A5AL, 0x00, 0x00);
        assert 0x5A5A5A5A5A5A0000L == id : "Incorrect id " + id;
        assert 0x5A5A5A5A5A5AL == g.millis(id) : "Incorrect millis " + g.millis(id);
        assert 0x00 == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0x00 == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void testSequence() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 8, 8);
        long id = g.generate(0x00L, 0x5A, 0x00);
        assert 0x5A00L == id : "Incorrect id " + id;
        assert 0x00L == g.millis(id) : "Incorrect millis " + g.millis(id);
        assert 0x5A == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0x00 == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void testPartition() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 8, 8);
        long id = g.generate(0x00L, 0x00, 0x5A);
        assert 0x5AL == id : "Incorrect id " + id;
        assert 0x00L == g.millis(id) : "Incorrect millis " + g.millis(id);
        assert 0x00 == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0x5A == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void testState() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 8, 8)
                .withSequence(0x5A)
                .withPartition(0xA5);
        long id = g.generate();
        assert 0xA5 == g.partition(id) : "Incorrect partition " + g.partition(id);
        assert 0x5A == g.sequence(id) : "Incorrect sequence " + g.sequence(id);

        long id2 = g.generate();
        assert 0xA5 == g.partition(id2) : "Incorrect partition " + g.partition(id2);
        assert 0x5B == g.sequence(id2) : "Incorrect sequence " + g.sequence(id2);
    }

    @Test
    public void test8_8() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 8, 8);
        long id = g.generate(0x5A5A5A5A5A5AL, 0x5a, 0x5a);
        assert 0x5A5A5A5A5A5A5A5AL == id : "Incorrect id " + id;
        assert 0x5A == g.partition(id) : "Incorrect partition " + g.partition(id);
        assert 0x5A == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0x5A5A5A5A5A5AL == g.millis(id) : "Incorrect millis " + g.millis(id);
    }

    @Test
    public void test_10_8() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 10, 8);
        long id = g.generate(0x5A5A5A5A5L, 0x25A, 0xA5);
        assert 0x16969696965AA5L == id : "Incorrect id " + id;
        assert 0x25A == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0xA5 == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void test_10_13() {
        final FlexId g = new FlexId(INSTAGRAM_EPOCH, 10, 13);
        long id = g.generate(0x5A5A5A5A5L, 0x25A, 0xA5);
        assert 0x2D2D2D2D2CB40A5L == id : "Incorrect id " + id;
        assert 0x25A == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0xA5 == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void test_0_0() {
        final FlexId g = new FlexId(INSTAGRAM_EPOCH, 0, 0);
        long id = g.generate(0x5A5A5A5A5L, 0x25a, 0xa5);
        assert 0x5A5A5A5A5L == id : "Incorrect id " + id;
        assert 0x00 == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0x00 == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void test_Default() {
        final FlexId g = new FlexId();
        long id = g.generate();
        assert (System.currentTimeMillis() << 16) - id < 1 : "Incorrect id " + id;
        assert 0x00 == g.sequence(id) : "Incorrect sequence " + g.sequence(id);
        assert 0x00 == g.partition(id) : "Incorrect partition " + g.partition(id);
    }

    @Test
    public void test_Timestamp_DefaultEpoch() {
        final FlexId g = new FlexId();
        long id = g.generate();
        assert Instant.now().compareTo(g.timestamp(id).toInstant()) < 1000 : "Incorrect timestamp " + g.timestamp(id);
    }

    @Test
    public void test_Timestamp_CustomEpoch() {
        final FlexId g = new FlexId(FlexId.DEFAULT_EPOCH, 8, 8);
        long id = g.generate();
        assert Instant.now().compareTo(g.timestamp(id).toInstant()) < 1000 : "Incorrect timestamp " + g.timestamp(id);
    }
}
