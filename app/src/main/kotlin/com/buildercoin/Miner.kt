package com.buildercoin

import com.google.common.base.Preconditions.checkArgument
import com.google.common.io.BaseEncoding
import io.micronaut.context.annotation.Context
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import mu.KotlinLogging
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

    private val logger = KotlinLogging.logger { }

    private val blockReward: CAmount = 50

    private val blockchain = mutableListOf<CBlockHeader>()

    private val unverifiedTransactions = mutableListOf<CTransaction>()
    private val verifiedTransactions = mutableMapOf<Hash, CTransaction>()

    private val unspentTransactionOutputs = mutableMapOf<COutPoint, CTxOut>()

    @PostConstruct
    fun startMining() = thread { mine() }

    @Get("/blocks")
    fun getBlocks() = blockchain

    @Get("/transactions")
    fun getTransactions() = verifiedTransactions

    @Get("/transactions/{hash}")
    fun getTransaction(hash: Hash) = verifiedTransactions[hash]

    fun mine() {
        logger.info { "Started mining" }

        while (true) {
            val transactions = unverifiedTransactions.toMutableList()

            val fee = transactions.sumOf { calculateFee(it) }

            val scriptPubKey: Script = "coins for gootmand@"

            transactions.add(
                CTransaction(
                    inputs = listOf(CTxIn(COutPoint("", 0u), "Getting paid", 0u)),
                    outputs = listOf(CTxOut(blockReward + fee, scriptPubKey))
                )
            )
            val hashMerkleRoot = transactions.hash()

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
                            unspentTransactionOutputs[COutPoint(hash, index.toUInt())] = cTxOut
                        }
                        transaction.inputs.forEach { unspentTransactionOutputs.remove(it.previousOutput) }
                        verifiedTransactions[hash] = transaction
                    }
                    break
                }

                if (unverifiedTransactions.size != transactions.size) {
                    break
                }
            }
        }
    }

    @Post("/transactions")
    fun addTransaction(transaction: CTransaction) {
        checkArgument(transaction.version == 2u, "Version must be 2")
        checkArgument(transaction.inputs.isNotEmpty(), "Transaction must have inputs")
        checkArgument(transaction.outputs.isNotEmpty(), "Transaction must have outputs")

        validate(transaction)

        unverifiedTransactions.add(transaction)
    }

    private fun validate(transaction: CTransaction) {
        transaction.inputs.forEach { input ->
            val previousOutputPointer = input.previousOutput

            val previousOutput = unspentTransactionOutputs[previousOutputPointer]
                ?: throw IllegalStateException("Unspent transaction not found: $previousOutputPointer")

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
        val totalInput: CAmount = transaction.inputs.sumOf { input -> unspentTransactionOutputs[input.previousOutput]!!.value }
        val totalOutput: CAmount = transaction.outputs.sumOf { it.value }

        return totalInput - totalOutput
    }
}

