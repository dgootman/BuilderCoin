package com.buildercoin

import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

typealias CAmount = Long

typealias Script = String // TODO: Script is actually a very sophisticated construct. Requires further research.
typealias Hash = String // TODO: Script is actually a very sophisticated construct. Requires further research.

/**
 * A BitCoin Transaction.
 *
 * See [transaction.h](https://github.com/bitcoin/bitcoin/blob/master/src/primitives/transaction.h).
 */
data class CTransaction(
    @NotNull val version: UInt = 2u,
    @NotEmpty @Valid val inputs: List<CTxIn>,
    @NotEmpty @Valid val outputs: List<CTxOut>,
    @NotNull val lockTime: UInt = 0u,
)

/** An input of a transaction.  It contains the location of the previous
 * transaction's output that it claims and a signature that matches the
 * output's public key.
 */
data class CTxIn(
    @NotNull @Valid val previousOutput: COutPoint,
    @NotNull @NotEmpty val scriptSig: Script,
    val sequence: UInt
)

/** An output of a transaction.  It contains the public key that the next input
 * must be able to sign with to claim it.
 */
data class CTxOut(
    @Positive val value: CAmount,
    @NotNull @NotEmpty val scriptPubKey: Script
)

/** An outpoint - a combination of a transaction hash and an index n into its vout */
data class COutPoint(
    @NotNull val hash: Hash,
    @PositiveOrZero val n: UInt
)