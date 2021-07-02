package com.buildercoin

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Preconditions.checkArgument
import com.google.common.io.BaseEncoding
import io.micronaut.context.annotation.Context
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import mu.KotlinLogging
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import javax.annotation.PostConstruct
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextUInt

@Context
@Singleton
@Controller("/")
class Miner {
    data class Configuration(
        val peers: MutableSet<URI>
    ) {
        companion object {
            private val yaml = YAMLMapper().registerModule(KotlinModule())!!

            private val configurationFile = File("miner.yml")

            fun load() = yaml.readValue<Configuration>(configurationFile).also { it.save() }
        }

        fun save() {
            yaml.writeValue(configurationFile, this)
        }
    }

    private val logger = KotlinLogging.logger { }

    private val blockReward: CAmount = 50

    private var blockchain = mutableListOf<CBlockHeader>()

    // CTxMemPool stores valid-according-to-the-current-best-chain transactions that may be included in the next block.
    private val mempool: CTxMemPool = mutableListOf()

    // Unspent Transaction Outputs
    private val utxo = mutableMapOf<COutPoint, CTxOut>()

    private val verifiedTransactions = mutableMapOf<Hash, CTransaction>()

    private val configuration = Configuration.load()

    @PostConstruct
    fun startMining() = thread { mine() }

    @Get("/blocks")
    fun getBlocks() = blockchain

    @Get("/transactions")
    fun getTransactions() = verifiedTransactions

    @Get("/transactions/{hash}")
    fun getTransaction(hash: Hash) = verifiedTransactions[hash]

    @Get("/peers")
    fun getPeers() = configuration.peers

    @Post("/peers")
    fun addPeer(peer: URI) {
        peer.resolve("/blocks").get<MutableList<CBlockHeader>>()

        configuration.peers.add(peer)
        configuration.save()
    }

    @Delete("/peers/{peer}")
    fun deletePeer(peer: URI) {
        if (configuration.peers.remove(peer)) {
            configuration.save()
        }
    }

    private fun mine() {
        logger.info { "Started mining" }

        configuration.peers.forEach { peer ->
            try {
                val peerChain = peer.resolve("/blocks").get<MutableList<CBlockHeader>>()

                if (peerChain.size > blockchain.size) {
                    var prevHeader: CBlockHeader? = null
                    for (header in peerChain) {
                        if (prevHeader != null) {
                            checkArgument(header.hashPrevBlock == prevHeader.hash())
                        }
                        prevHeader = header
                    }

                    blockchain = peerChain
                }
            } catch (e: Exception) {
                logger.warn("Failed to sync with peer: $peer", e)
            }
        }

        while (true) {
            val transactions = mempool.toMutableList()

            val fee = transactions.sumOf { calculateFee(it) }

            val scriptPubKey: Script = "coins for gootmand@"

            transactions.add(
                CTransaction(
                    inputs = listOf(CTxIn(COutPoint("", 0u), "Getting paid", 0u)),
                    outputs = listOf(CTxOut(blockReward + fee, scriptPubKey))
                )
            )
            val hashMerkleRoot = transactions.hash() // That's not how Merkle Trees work IRL

            var attempts = 0
            while (true) {
                attempts++
                val blockHeader = CBlockHeader(
                    hashPrevBlock = blockchain.lastOrNull()?.hash() ?: "",
                    hashMerkleRoot = hashMerkleRoot,
                    bits = UInt.MAX_VALUE / 1_000_000u, // Grossly oversimplified
                    nonce = Random.nextUInt()
                )
                val hashOutput = ByteBuffer.wrap(BaseEncoding.base64().decode(blockHeader.hash())).int.toUInt()
                if (hashOutput < blockHeader.bits) {
                    blockchain.add(blockHeader)
                    transactions.forEach { transaction ->
                        val hash = transaction.hash()
                        transaction.outputs.forEachIndexed { index, cTxOut ->
                            utxo[COutPoint(hash, index.toUInt())] = cTxOut
                        }
                        transaction.inputs.forEach { utxo.remove(it.previousOutput) }
                        verifiedTransactions[hash] = transaction
                    }
                    break
                }

                if (mempool.size != transactions.size) {
                    break
                }
            }
        }
    }

    @Post("/transactions")
    fun addTransaction(@Body input: String) {
        val transaction = GSON.fromJson<CTransaction>(input)
        checkArgument(transaction.version == 2u, "Version must be 2")
        checkArgument(!transaction.inputs.isNullOrEmpty(), "Transaction must have inputs")
        checkArgument(!transaction.outputs.isNullOrEmpty(), "Transaction must have outputs")

        validate(transaction)

        mempool.add(transaction)
    }

    private fun validate(transaction: CTransaction) {
        transaction.inputs.forEach { input ->
            val previousOutputPointer = input.previousOutput

            val previousOutput = utxo[previousOutputPointer]
                ?: throw IllegalStateException("Unspent transaction not found: $previousOutputPointer")

            // Completely nonsensical "script validation"
            if (previousOutput.scriptPubKey != input.scriptSig) {
                throw IllegalArgumentException("Failed to verify signature: ${previousOutput.scriptPubKey} != ${input.scriptSig}")
            }
        }

        val fee = calculateFee(transaction)

        if (fee <= 0) {
            throw IllegalArgumentException("I refuse to pay for your transaction and my service isn't free: $fee")
        }
    }

    private fun calculateFee(transaction: CTransaction): CAmount {
        val totalInput: CAmount = transaction.inputs.sumOf { input -> utxo[input.previousOutput]!!.value }
        val totalOutput: CAmount = transaction.outputs.sumOf { it.value }

        return totalInput - totalOutput
    }
}