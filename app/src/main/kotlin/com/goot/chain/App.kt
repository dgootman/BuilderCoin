package com.goot.chain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalUnsignedTypes::class)
data class Block(
    val version: UInt,
    val hashPrevBlock: ByteArray,
    val hashMerkleRoot: ByteArray,
    val time: Long,
    val bits: UInt,
    val nonce: UInt
)
//
//    fun mine() {
//        while (hash == null) {
//            nonce = Random.nextUInt()
//            val message = data + previousHash + nonce.toString()
//
//            val hashCode = Hashing.sha1().hashBytes(message.toByteArray())
//            attempts++
//            if (hashCode.asInt().toUInt() < UInt.MAX_VALUE / 10000000u) {
//                hash = BaseEncoding.base64().encode(hashCode.asBytes())
//            }
//        }
//    }
//}

class BlockChain {

}

@OptIn(ExperimentalTime::class)
class Miner(private val kinesis: KinesisClient) {
    private val logger = KotlinLogging.logger { }

    private var jobs: List<Job>? = null

    private val blocks = emptyList<Block>()

    fun start() {
        val shards = kinesis.listShards { it.streamName("GootChain-Blocks") }

        jobs = shards.shards().map { shard ->
            GlobalScope.launch {
                val shardIteratorResponse = kinesis.getShardIterator {
                    it.streamName("GootChain-Blocks")
                        .shardId(shard.shardId())
                        .shardIteratorType(ShardIteratorType.TRIM_HORIZON)
                }
                var shardIterator = shardIteratorResponse.shardIterator()
                while (shardIterator != null) {
                    logger.trace { "Shard iterator: $shardIterator" }
                    val records = kinesis.getRecords { it.shardIterator(shardIterator) }
                    records.records().forEach {
                        logger.info { "New record: $it" }
                        val block: Block = jacksonObjectMapper().readValue(it.data().asUtf8String())
                        logger.info { "Block: $block" }
                    }
                    shardIterator = records.nextShardIterator()
                    delay(1.seconds)
                }
            }
        }
    }

    suspend fun wait() {
        jobs?.forEach { it.join() }
    }

    fun stop() {
        jobs?.forEach { it.cancel() }
        jobs = null
    }
}

class Verifier() {
    fun start() {

    }
}

suspend fun main() {
    Miner(KinesisClient.create()).run {
        start()
        wait()
    }
}