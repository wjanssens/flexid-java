import org.testng.annotations.Test;

import java.time.Instant;

public class FlexIdTest {

    private static final long UNIX_EPOCH = 0L;
    private static final long CUSTOM_EPOCH = 1420070400000L;


    @Test
    public void testEpoch() {
        final FlexId g = new FlexId(CUSTOM_EPOCH, 4, 4, 0);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x03 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testDefaults() {
        final FlexId g = new FlexId();
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0xD3 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testFours() {
        final FlexId g = new FlexId(UNIX_EPOCH, 4, 4, 0);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x03 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testOdds() {
        final FlexId g = new FlexId(UNIX_EPOCH, 5, 5, 0);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x13 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testSixes() {
        final FlexId g = new FlexId(UNIX_EPOCH, 6, 6, 0);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x13 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);

        long id2 = g.generate("test");
        assert 0x01 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testZeros() {
        final FlexId g = new FlexId(UNIX_EPOCH, 0, 0, 0);
        final Instant now = Instant.now();

        long id = g.generate("test");
        assert now.toEpochMilli() - g.extractMillis(id) < 5 : "Incorrect millis " + g.extractMillis(id) + " != " + now.toEpochMilli();
        assert now.compareTo(g.extractTimestamp(id).toInstant()) < 5000 : "Incorrect timestamp " + g.extractTimestamp(id);
        assert 0x00 == g.extractSequence(id) : "Incorrect sequence " + g.extractSequence(id);
        assert 0x00 == g.extractShard(id) : "Incorrect shard " + g.extractShard(id);

        long id2 = g.generate("test");
        assert 0x00 == g.extractSequence(id2) : "Incorrect sequence " + g.extractSequence(id2);
    }

    @Test
    public void testChecksum() {
        assert FlexId.validateLuhn16(0x00000000);
        assert 0 == FlexId.generateLuhn16(0x00000000);
        assert 0x7ffffff7 == FlexId.generateLuhn16(0x7ffffff0);
        assert !FlexId.validateLuhn16(0x7ffffff0);
        assert !FlexId.validateLuhn16(0x7ffffff1);
        assert !FlexId.validateLuhn16(0x7ffffff2);
        assert !FlexId.validateLuhn16(0x7ffffff3);
        assert !FlexId.validateLuhn16(0x7ffffff4);
        assert !FlexId.validateLuhn16(0x7ffffff5);
        assert !FlexId.validateLuhn16(0x7ffffff6);
        assert FlexId.validateLuhn16(0x7ffffff7);
        assert !FlexId.validateLuhn16(0x7ffffff8);
        assert !FlexId.validateLuhn16(0x7ffffff9);
        assert !FlexId.validateLuhn16(0x7ffffffa);
        assert !FlexId.validateLuhn16(0x7ffffffb);
        assert !FlexId.validateLuhn16(0x7ffffffc);
        assert !FlexId.validateLuhn16(0x7ffffffd);
        assert !FlexId.validateLuhn16(0x7ffffffe);
    }

    @Test
    public void testSpeed() {
        final long start = System.currentTimeMillis();
        final FlexId id = new FlexId(0, 8, 8, 0);
        for (int i = 0; i < 256; i++) {
            id.generate(0);
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
