package com.solana.rpc

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction

fun buildMemoTransaction(address: SolanaPublicKey, memo: String) =
    TransactionInstruction(SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"),
        listOf(AccountMeta(address, true, true)),
        memo.encodeToByteArray()
    )