package br.com.ergoperitus.seguranca

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class SenhaProtegida(
    val hash: String,
    val salt: String
)

object ProtecaoSenha {

    private const val TAMANHO_SALT_BYTES = 16
    private const val TAMANHO_CHAVE_BITS = 256
    private const val ITERACOES = 310_000

    /**
     * Transforma a senha em um valor que pode ser salvo sem revelar o texto
     * digitado. O salt faz com que duas pessoas com a mesma senha produzam
     * resultados diferentes no banco.
     */
    fun proteger(senha: CharArray): SenhaProtegida {
        val salt = ByteArray(TAMANHO_SALT_BYTES).also {
            SecureRandom().nextBytes(it)
        }

        val hash = derivarChave(senha, salt)

        return SenhaProtegida(
            hash = Base64.encodeToString(hash, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    /**
     * Para conferir o login, calculamos novamente o hash usando o salt salvo.
     * A comparação em tempo constante reduz a exposição a ataques de timing.
     */
    fun corresponde(
        senhaInformada: CharArray,
        hashSalvo: String,
        saltSalvo: String
    ): Boolean {
        return try {
            val salt = Base64.decode(saltSalvo, Base64.NO_WRAP)
            val hashEsperado = Base64.decode(hashSalvo, Base64.NO_WRAP)
            val hashInformado = derivarChave(senhaInformada, salt)

            MessageDigest.isEqual(hashEsperado, hashInformado)
        } catch (_: IllegalArgumentException) {
            // Um registro corrompido jamais deve liberar o acesso.
            false
        }
    }

    private fun derivarChave(
        senha: CharArray,
        salt: ByteArray
    ): ByteArray {
        val especificacao = PBEKeySpec(
            senha,
            salt,
            ITERACOES,
            TAMANHO_CHAVE_BITS
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(especificacao)
                .encoded
        } finally {
            especificacao.clearPassword()
            senha.fill('\u0000')
        }
    }
}
