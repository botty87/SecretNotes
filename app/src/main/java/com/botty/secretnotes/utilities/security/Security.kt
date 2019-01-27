package com.botty.secretnotes.utilities.security

import com.botty.secretnotes.storage.new_db.note.Note
import com.botty.secretnotes.utilities.logException
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.PwHash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.utils.Key
import java.security.GeneralSecurityException


object Security {

    fun passwordMatch(note: Note, userPassword: String): Boolean {
        return Security.passwordMatch(note.passwordHash, userPassword)
    }

    @Suppress("KotlinDeprecation") //Useful for old note migration
    fun passwordMatch(passwordHash: String?, userPassword: String): Boolean {
        val lazySodium = LazySodiumAndroid(SodiumAndroid())
        return lazySodium.cryptoPwHashStrVerify(passwordHash, userPassword)
    }

    fun getPasswordHash(password: String): String {
        val lazySodium = LazySodiumAndroid(SodiumAndroid())
        return lazySodium.cryptoPwHashStr(password, PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN)
    }

    fun decryptNote(password: String, note: Note): String {
        try {
            val lazySodium = LazySodiumAndroid(SodiumAndroid())
            val key = Key.fromHexString(lazySodium.cryptoGenericHash(password))
            return lazySodium.cryptoSecretBoxOpenEasy(note.content, note.nonceArray, key)
        } catch (e: Exception) {
            logException(e)
            throw e
        }
    }

    fun encryptNote(password: String, content: String): Pair<String, ByteArray> {
        try {
            val lazySodium = LazySodiumAndroid(SodiumAndroid())
            val nonce = lazySodium.randomBytesBuf(SecretBox.NONCEBYTES)
            val passwordHash = lazySodium.cryptoGenericHash(password)
            val key = Key.fromHexString(passwordHash)
            val contentCrypt = lazySodium.cryptoSecretBoxEasy(content, nonce, key)

            return contentCrypt to nonce
        } catch (e: GeneralSecurityException) {
            logException(e)
            throw e
        }

    }

    const val MASTER_PAS_TO_SET_KEY = "mas_pas_set"
    const val MASTER_PAS_KEY = "mas_pas"
    const val AUTO_LOCK_KEY = "auto_lock"
}