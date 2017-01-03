This library is inspired by two different id generation strategies: type 1 UUIDs, and Instragam's Ids.

* Type 1 UUIDs have 60 bits for time, 14 bits for clock sequence, 48 bits for node, 4 version bits, 2 variant bits
    * 128 bit numbers are difficult to deal with in most programming languages and storage mediums
    * 60 bits of ms results in a rollover of 36558901 years which seems like overkill
    * 14 bits clock sequence allows for 32767 ids per millisecond
    * 48 bits for node allows for 281474976710655 unique nodes (i.e. MAC addresses)
* Instragram IDs have 41 bits for time, 13 bits for shard id, 10 bits for sequence
    * 64 bit numbers are perfect for languages and storage mediums that only support 64 bit integers
    * 41 bits for ms results in negative values in 34 years, and a rollover in only 69 years
      from their custom epoch of Jan 1, 2011 (i.e. 2080)
      which seems like insufficient longevity
    * 13 bits for shard allows 8191 shards which seems like overkill
    * 10 bits sequence allows for 1024 ids per millisecond which seems very reasonable
* Mongo IDs have 32 bits for time, 24 bits for machine ID, 16 bits for process ID, 24 bytes for counter
    * 96 bit numbers seem don't fit nicely into any language type system
    * 96 bit numbers cannot be obfuscated with HashId
    * 32 bits for seconds since UNIX epoch results in rollover in 136 years since epoch (i.e. 2106) 
      which seems like insufficient longevity
    * 24 bits for counter results in 4294967296 IDs per second per machine which seems like overkill
* By default, FlexIDs have 48 bits for time, 8 bits for sequence, 8 bits for partition
    * 64 bit numbers are perfect for languages and storage mediums that only support 64 bit integers
    * 48 bits for ms results in negative values in 4462 years, and a rollover in 8925 years
    * 8 bits sequence allows for 256 ids per millisecond
    * 8 bits for partition allows for 256 partitions
    * 8 bit values for sequence and partition results in IDs where all components are clearly visible in the output 

The exact division is configurable, but the following guidelines should be followed:
* time should be 43-47 bits, for between 557 - 8925 of positive values 
  (44-48 bits for between 1115 and 17851 years with negative values)
  * The HashIds algorithm doesn't work with negative values, which needs to be considered
    when deciding how many bits to assign to time since many languages only has unsigned integers.
* sequence should be 4-12 bits, for between 16 and 4096 ids/ms/partition
* partition should be 4-12 bits, for between 16 and 4096 partitions
* the most common configurations are 47/8/8 and 45/10/8 but you should evaluate your longevity and scalability 
  requirements and set the values accordingly.
* There is no minimum number of bits for sequence or partition, so you could set them to 0 but this is not recommended, 
  even in a single node system, as it would not allow for any future expansion.
* It may be desirable to use 4 bits, 8 bits, or 12 bits for sequence and partition as that will result
  in IDs that allow you to directly read the input values.

The order of the components is debatable, but this implementation adopts the same order as type 1 UUIDs which 
results in an order of decreasing change; time, sequence, partition.  
This differs from the order adopted by Instagram, so this library cannot be used to generate Instagram compatible Ids.

There are many ways that you may decide to partition your ID space:
* By Entity: Splitting up your ID space base on table or class
    * If all entities share an ID space then you don't need to reserve
      any space for them in the partition field.  This is the preferred scenario.
    * If it's desirable to identify the type of entity from the ID then some number of bits of the partition need
      to be assigned to identify the entity.
* By Domain: Splitting up a database based on business domain.
    * If domains are completely independent and it's OK for IDs to overlap between domains then
      none of the partition bits need to be assigned to identify the domain.
      This is the preferred scenario.
    * If domains share an ID space then some number of bits of the partition field should be 
      assigned to identify the domain and prevent collisions between domains.
* By Shard: Splitting up a database row-wise, typically by user.
    * Each shard needs to be assigned a unique ID and data is mapped onto a shard.
    * The number of physical shards 
* By Cluster: Using multiple application servers to access the same database.
    * If your application servers are stateless and don't assign IDs then you don't need to reserve
      any space for them in the partition field.  This is the preferred scenario.
    * If your application servers assign IDs then some number of bits of the partition need to be assigned
      to identify the server to prevent collisions between application servers.
    * As more bits are allocated to cluster, fewer bits may be needed for sequence.
    
Other strategies for generating IDs that aren't very interesting are:
* Type 4 Random UUID
    * chance of collision only increases as data grows
    * not sequential
    * 128-bits
* High/Low
    * has single point of failure when new high is required
* Linear Chunk
    * has single point of failure when new chunk is required
* DB Serial Columns
    * vendor specific implementations differ on the timing of the id generation
    * must be assigned by the database
    * not applicable for NoSQL applications
* Max+1
    * can be expensive (slow) as data grows
    * not applicable for NoSQL applications
* Twitter Snowflake
    * retired
    * requires additional server infrastructure

Further Reading:
* https://engineering.instagram.com/sharding-ids-at-instagram-1cf5a71e5a5c#.vdwtymfgu
* http://code.flickr.net/2010/02/08/ticket-servers-distributed-unique-primary-keys-on-the-cheap/
* http://rob.conery.io/2014/05/29/a-better-id-generator-for-postgresql/
* http://hashids.org/
