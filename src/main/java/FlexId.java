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
 * Generates 64-bit integer ids with a time component, sequence number component, and partition component.
 *
 * This system of generating IDs requires no central authority to generate key values.
 *
 * Since the time component of the ID is the first bits of the integer IDs are inherently sortable.
 * This is advantage for storage and retrieval of rows in a database, but may be a disadvantage if it's not desirable to
 * expose the sorted identifiers in URLs.  To overcome this limitation it is recommended to translate IDs before showing
 * them to users by using HashIds (http://hashids.org/) or Knuth's Integer Hash.
 * The time component of the id locates the ID in time.
 *
 *
 * <li>the time value will roll over every 2231 years
 * <li>since longs are signed in Java, negative ids will appear in 1115 years
 * <li>the time is the first part of the key to facilitate an implied temporal order to IDs.
 * The sequence component of the id allows up to 1024 ids to be generated per millisecond per partition.
 * <li>this implementation keeps a serial counter for sequence
 * <li>other stateless implementations may use a random serial value
 * The partition component of the id has one of the following uses:
 * <li>guarantee uniqueness across partitions that may have time drift
 * <li>create a pool of generators capable of generating more than 1024 IDs per millisecond
 * <li>compute a shard value that may be used to locate data onto to a particular node
 *
 *
 */
public class FlexId {
    private static final Logger logger = Logger.getLogger(FlexId.class.getName());

    public static final long UNIX_EPOCH = 0L;
    public static final long DEFAULT_EPOCH = 1420070400000L;
    public static final long INSTAGRAM_EPOCH = 1293840000000L;

    private final int sequenceBits;
    private final int partitionBits;
    private final int sequenceMask;
    private final int partitionMask;
    private final long epoch;
    private int sequence =  0;
    private int partition = 0;

    /**
     * Creates an ID generator.
     * @param epoch the start date for the time component.
     * @param sequenceBits the number of sequence bits for avoiding sub-millisecond id collisions; recommend between 8 and 12.
     * @param partitionBits the number of partition bits for identifying nodes/shards; recommend between 6 and 8.
     */
    public FlexId(long epoch, int sequenceBits, int partitionBits) {
        this.epoch = epoch;
        this.sequenceBits = sequenceBits;
        this.partitionBits = partitionBits;

        int mask = 0;
        for (int i = 0; i < sequenceBits; i++) {
            mask = (mask << 1) | 1;
        }
        this.sequenceMask = mask;
        mask = 0;
        for (int i = 0; i < partitionBits; i++) {
            mask = (mask << 1) | 1;
        }
        this.partitionMask = mask;

        final long millis = (long) Math.pow(2, 64 - sequenceBits - partitionBits - 1);
        final long years = millis / 1000 / 60 / 60 / 24 / 365;
        final OffsetDateTime start = Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = start.plus(millis, ChronoUnit.MILLIS);

        logger.info(String.format("Ids have a time range of %d years (%s to %s), %d sequences, %d partitions",
                years,
                start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                (int) Math.pow(2, sequenceBits),
                (int) Math.pow(2, partitionBits)));
    }

    /**
     * Creates a generator with 8 bits sequences for 256 ids per millisecond, and 8 bit partitions for for 256 partitions,
     * leaving 47 bits times for 4462 years of positive values; and and epoch of 1970-01-01T00:00:00Z;
     */
    public FlexId() {
        this(UNIX_EPOCH, 8, 8);
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
     */
    public FlexId withSequence(int sequence) {
        this.sequence = sequence;
        return this;
    }

    public int getSequence() {
        return this.sequence;
    }

    /**
     * Randomizes the partition of the generator to a random value.
     * Use this method when you don't have any other reasonable way of partitioning keys.
     */
    public FlexId withRandomPartition() {
        return withPartition(new Random().nextInt());
    }

    /**
     * Sets the partition of the generator.
     */
    public FlexId withPartition(int partition) {
        this.partition = partition;
        return this;
    }

    public int getPartition() {
        return this.partition;
    }


    /**
     * Generates an ID with the supplied millis, supplied sequence, and supplied partition.
     * This method uses the raw millis value and does not adjust for the configured epoch.
     * @param millis the number of milliseconds since epoch
     */
    public long generate(long millis, int sequence, int partition) {
        long result = millis << (sequenceBits + partitionBits);
        result |= (sequence & sequenceMask) << partitionBits;
        result |= (partition & partitionMask);
        return result;
    }

    /**
     * Generates an ID with generated millis, supplied sequence, and supplied partition.
     */
    public long generate(int sequence, int partition) {
        return generate(System.currentTimeMillis() - epoch, sequence, partition);
    }

    /**
     * Generates an ID with generated millis, next sequence value, and supplied partition.
     */
    public long generate(int partition) {
        return generate(sequence++, partition);
    }

    /**
     * Generates an ID with generated millis, next sequence sequence value, and configured partition.
     */
    public long generate() {
        return generate(partition);
    }

    /**
     * Extracts the millis component of an ID.
     */
    public long millis(long id) {
        return id >> (sequenceBits + partitionBits);
    }

    /**
     * Extracts the sequence component of an ID.
     */
    public int sequence(long id) {
        return (int) ((id >> partitionBits) & sequenceMask);
    }

    /**
     * Extracts the partition component of an ID.
     */
    public int partition(long id) {
        return (int) (id & partitionMask);
    }

    /**
     * Extracts the least significant bits of the partition component of an ID.
     * This serves as a simple method for mapping a large logical partition space onto a smaller physical partition space.
     * @param id the ID to extract from
     * @param bits the number of bits to extract
     * @throws IllegalArgumentException if the bits parameter is greater than the number of partition bits of the Id.
     */
    public int partition(long id, int bits) {
        if (bits > partitionBits) {
            throw new IllegalArgumentException("bits must be <= the partition bits of the Id.");
        }
        int mask = 0;
        for (int i = 0; i < bits; i++) {
            mask = (mask << 1) | 1;
        }
        return partition(id) & mask;
    }

    /**
     * A convenience method for computing a partition value from a string using an SHA-256 hash.
     * This would typically be used to compute a shard id from a string identifier.
     */
    public static int sha256(String text) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(text.getBytes("UTF-8"));
            return ByteBuffer.wrap(hash).getInt();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
