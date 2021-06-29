package com.buildercoin

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * A BitCoin Block Header.
 *
 * See [block.h](https://github.com/bitcoin/bitcoin/blob/master/src/primitives/block.h).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CBlockHeader(
    val version: UInt = 2u,
    val hashPrevBlock: Hash,
    val hashMerkleRoot: Hash,
    val time: Long = Instant.now().epochSecond,
    val bits: UInt,
    val nonce: UInt,
)

/**
 * A BitCoin Block.
 *
 * See [block.h](https://github.com/bitcoin/bitcoin/blob/master/src/primitives/block.h).
 */
data class CBlock(
    val header: CBlockHeader,
    val transactions: MutableList<CTransaction> = mutableListOf()
)