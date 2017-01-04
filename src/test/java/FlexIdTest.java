import org.testng.annotations.Test;

import java.time.Instant;

public class FlexIdTest {

    private static final long UNIX_EPOCH = 0L;
    private static final long CUSTOM_EPOCH = 1420070400000L;


    @Test
    public void testEpoch() {
        final FlexId g = new FlexId(CUSTOM_EPOCH, 4, 4, 4)
                .withConstant(0x5A);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x0B == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);
        assert 0x0A == g.extractConstant(id) : "Incorrect constant " + g.extractConstant(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testDefaults() {
        final FlexId g = new FlexId()
                .withConstant(0x5A);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x1B == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);
        assert 0x00 == g.extractConstant(id) : "Incorrect constant " + g.extractConstant(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testFours() {
        final FlexId g = new FlexId(UNIX_EPOCH, 4, 4, 4)
                .withConstant(0x5A);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x0B == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);
        assert 0x0A == g.extractConstant(id) : "Incorrect constant " + g.extractConstant(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testOdds() {
        final FlexId g = new FlexId(UNIX_EPOCH, 5, 5, 5)
                .withConstant(0x5A);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x1B == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);
        assert 0x1A == g.extractConstant(id) : "Incorrect constant " + g.extractConstant(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testSixes() {
        final FlexId g = new FlexId(UNIX_EPOCH, 6, 6, 6)
                .withConstant(0x5A);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x1B == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);
        assert 0x1A == g.extractConstant(id) : "Incorrect constant " + g.extractConstant(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testZeros() {
        final FlexId g = new FlexId(UNIX_EPOCH, 0, 0, 0)
                .withConstant(0x5A);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x00 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);
        assert 0x00 == g.extractConstant(id) : "Incorrect constant " + g.extractConstant(id);

        long id2 = g.generate("test");
        assert 0x00 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }
}
