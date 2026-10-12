package com.purrspa.system.data

import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, password-protected archive envelope. The inner payload is the existing ZIP.
 * A fresh salt and GCM nonce are generated for every export. Passwords and raw keys
 * are never persisted. Decryption is fully authenticated BEFORE parsing the ZIP.
 */
object BackupCrypto {
    private val magic = "PSBACKUP2".toByteArray(Charsets.US_ASCII)
    private const val iterations = 250_000
    private const val saltSize = 16
    private const val nonceSize = 12
    private const val maxArchiveBytes = 1024L * 1024 * 1024

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size >= 12) { "Backup password must have at least 12 characters" }
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val raw = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return try {
            SecretKeySpec(raw, "AES")
        } finally {
            raw.fill(0)
        }
    }

    fun encrypt(destination: OutputStream, password: CharArray): OutputStream {
        val salt = ByteArray(saltSize).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(nonceSize).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic + salt + nonce)
        destination.write(magic)
        destination.write(salt)
        destination.write(nonce)
        return CipherOutputStream(destination, cipher)
    }

    /**
     * Authenticates the entire encrypted stream before returning a private temporary ZIP.
     * This avoids parsing unauthenticated GCM plaintext from CipherInputStream.
     * The caller MUST delete the returned file in a finally block.
     */
    fun decryptToTemporaryZip(source: InputStream, password: CharArray, cacheDir: File): File {
        val temp = File.createTempFile("purrspa-decrypted-", ".zip", cacheDir)
        try {
            source.use { input ->
                val data = DataInputStream(input)
                val fileMagic = ByteArray(magic.size).also { data.readFully(it) }
                require(fileMagic.contentEquals(magic)) {
                    "Unsupported or unencrypted backup. Export a new password-protected backup."
                }
                val salt = ByteArray(saltSize).also { data.readFully(it) }
                val nonce = ByteArray(nonceSize).also { data.readFully(it) }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
                cipher.updateAAD(fileMagic + salt + nonce)
                CipherInputStream(data, cipher).use { decrypted ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val read = decrypted.read(buffer)
                            if (read == -1) break
                            require(read.toLong() <= maxArchiveBytes - total) { "Backup exceeds 1 GB limit" }
                            output.write(buffer, 0, read)
                            total += read
                        }
                    }
                }
            }
            return temp
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }
}
