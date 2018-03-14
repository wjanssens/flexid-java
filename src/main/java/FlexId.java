import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.logging.Logger;


/**
 * Generates 64-bit integer ids with a time component, sequence number component, and shard component.
 *
 * This system of generating IDs requires no central authority to generate key values.
 *
 * Since the time component of the ID is the first bits of the integer, IDs are inherently sortable.
 * This is often an advantage for storage and retrieval of rows in a database since it makes entities sortable and
 * captures the creation timestamp without the use of an extra column, but may be a disadvantage if it's not desirable
 * to expose the sorted identifiers in URLs.  To overcome this limitation it is recommended to translate IDs before
 * showing them to users by using HashIds (http://hashids.org/) or Knuth's Integer Hash.
 */
public class FlexId {
    private static final Logger logger = Logger.getLogger(FlexId.class.getName());

    private final short sequenceBits;
    private final short shardBits;
    private final short sequenceMask;
    private final short shardMask;
    private final short checkBits;
    private final short checkMask;
    private final long epoch;
    private int sequence = 0;

    /**
     * Creates an ID generator.
     * @param epoch the start date for the time component.
     * @param sequenceBits the number of bits for avoiding sub-millisecond id collisions; recommend between 8 and 12.
     * @param shardBits the number of bits for identifying shard; recommend between 6 and 8.
     * @param checkBits is the number of bits for checksum; either 0 or 4.
     * @throws IllegalArgumentException if sequenceBits or shardBits are &lt; 0 or &gt; 15
     */
    public FlexId(long epoch, int sequenceBits, int shardBits, int checkBits) {
        if (sequenceBits < 0 || sequenceBits > 15) {
            throw new IllegalArgumentException("sequenceBits must be between 0 and 15");
        }
        if (shardBits < 0 || shardBits > 15) {
            throw new IllegalArgumentException("shardBits must be between 0 and 15");
        }
        if (!(checkBits == 0 || checkBits == 4)) {
            throw new IllegalArgumentException("checkBits must be be either 0 or 4");
        }

        this.epoch = epoch;

        this.sequenceBits = (short) sequenceBits;
        this.shardBits = (short) shardBits;

        this.sequenceMask = createMask(this.sequenceBits);
        this.shardMask = createMask(this.shardBits);

        this.checkBits = (short) checkBits;
        this.checkMask = createMask(this.checkBits);

        final long millis = (long) Math.pow(2, 64 - sequenceBits - shardBits - 1);
        final long years = millis / 1000 / 60 / 60 / 24 / 365;
        final OffsetDateTime start = Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = start.plus(millis, ChronoUnit.MILLIS);

        logger.info(String.format("Ids have a time range of %d years (%s to %s), %d sequences, %d shards",
                years,
                start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                (int) Math.pow(2, sequenceBits),
                (int) Math.pow(2, shardBits)));
    }

    private static short createMask(short bits) {
        int mask = 0;
        for (short i = 0; i < bits; i++) {
            mask = (mask << 1) | 1;
        }
        return (short) mask;
    }

    /**
     * Creates a generator with 8 bits sequences for 256 ids per millisecond,
     * 8 bit partitions for for 256 partitions, 0 bit checksums,
     * leaving 47 bits times for 4462 years of positive values; and and epoch of 1970-01-01T00:00:00Z;
     */
    public FlexId() {
        this(0, 8, 8, 0);
    }

    /**
     * Sets the sequence number of the generator to a random value.
     * Use this method when initializing a generator if you don't want sequence numbers to start from 0.
     */
    public FlexId withRandomSequence() {
        return withSequence(new Random().nextInt());
    }

    /**
     * Sets the sequence number of the generator.
     * Use this method to initialize a generator with a starting sequence number.
     */
    public FlexId withSequence(int sequence) {
        this.sequence = sequence;
        return this;
    }

    public int getSequence() {
        return this.sequence;
    }

    public long getEpoch() {
        return this.epoch;
    }

    /**
     * Generates an ID with the supplied millis, supplied sequence, and supplied shard.
     * This method uses the raw millis value and does not adjust for the configured epoch.
     * @param millis the number of milliseconds since epoch
     */
    protected long generate(long millis, int sequence, int shard) {
        long v = millis << (sequenceBits + shardBits + checkBits)
                | (sequence & sequenceMask) << (shardBits + checkBits)
                | (shard & shardMask) << (checkBits);
        return checkBits == 0 ? v : generateLuhn16(v);
    }

    /**
     * Generates an ID with generated millis, next sequence value, and calculated shard.
     */
    public synchronized long generate(String shardKey) {
        return generate(System.currentTimeMillis() - epoch, this.sequence++, sha256(shardKey));
    }

    /**
     * Generates an ID with generated millis, next sequence value, and supplied shard.
     */
    public synchronized long generate(int shard) {
        return generate(System.currentTimeMillis(), this.sequence++, shard);
    }

    /**
     * Extracts the date/time component of an ID.
     * This is the derived from the millis component of the ID with the configured epoch applied.
     */
    public OffsetDateTime extractTimestamp(long id) {
        return Instant.ofEpochMilli(extractMillis(id)).atOffset(ZoneOffset.UTC);
    }

    /**
     * Extracts the millis component of an ID.
     * This is the raw value and is not adjusted for epoch.
     */
    public long extractRawMillis(long id) {
        return id >> (sequenceBits + shardBits);
    }

    /**
     * Extracts the millis component of an ID.
     */
    public long extractMillis(long id) {
        return (id >> (sequenceBits + shardBits)) + epoch;
    }

    /**
     * Extracts the sequence component of an ID.
     */
    public int extractSequence(long id) {
        return (int) ((id >> shardBits) & sequenceMask);
    }

    /**
     * Extracts the shard component of an ID.
     */
    public int extractShard(long id) {
        return (int) (id & shardMask);
    }

    /**
     * Extracts the least significant bits of the shard component of an ID.
     * This serves as a simple method for mapping a large logical shard space onto a smaller physical shard space.
     * @param id the ID to extract from
     * @param bits the number of bits to extract
     * @throws IllegalArgumentException if the bits parameter is greater than the number of shard bits of the Id.
     */
    public int extractShard(long id, int bits) {
        if (bits > shardBits) {
            throw new IllegalArgumentException("bits must be <= the shard bits of the Id.");
        }
        return extractShard(id) & createMask((short) bits);
    }

    /**
     * A convenience method for computing a shard value from a string using an SHA-1 hash.
     * This would typically be used to compute a shard ID from a string identifier such as a username.
     */
    public static short sha256(String text) {
        if (text == null) {
            return 0;
        }
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            final byte[] hash = digest.digest(text.getBytes("UTF-8"));
            return ByteBuffer.wrap(hash).getShort(18);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static long generateLuhn16(long input) {
        int factor = 2;
        int sum = 0;
        long runningValue = input;

        for (int i = 0; i < 16; i++) {
            runningValue = runningValue >> 4;
            int codePoint = (int) (runningValue & 0xf);
            int addend = factor * codePoint;
            factor = (factor == 2) ? 1 : 2;
            addend = (addend / 0xf) + (addend % 0xf);
            sum += addend;
        }

        int remainder = sum % 0xf;
        int check = (0xf - remainder) % 0xf;
        return input | check;
    }

    public static boolean validateLuhn16(long input) {
        int factor = 1;
        int sum = 0;
        long runningValue = input;

        for (int i = 0; i < 16; i++) {
            int codePoint = (int) (runningValue & 0xf);
            int addend = factor * codePoint;
            factor = (factor == 2) ? 1 : 2;
            addend = (addend / 0xf) + (addend % 0xf);
            sum += addend;
            runningValue = runningValue >> 4;
        }

        int remainder = sum % 0xf;
        return (remainder == 0);
    }
}
