package br.com.ergoperitus.seguranca

import java.security.SecureRandom
import java.util.Locale

object CodigoRecuperacao {

    private val aleatorio = SecureRandom()
    private const val HEXADECIMAL = "0123456789ABCDEF"

    fun gerar(): String {
        val bytes = ByteArray(16)
        aleatorio.nextBytes(bytes)

        val codigo = buildString {
            for (byte in bytes) {
                val valor = byte.toInt() and 0xFF
                append(HEXADECIMAL[valor ushr 4])
                append(HEXADECIMAL[valor and 0x0F])
            }
        }

        return codigo.chunked(4).joinToString("-")
    }

    fun normalizar(codigo: String): String {
        return codigo
            .filterNot { it.isWhitespace() || it == '-' }
            .uppercase(Locale.ROOT)
    }

    fun formatoValido(codigo: String): Boolean {
        val normalizado = normalizar(codigo)

        return normalizado.length == 32 &&
                normalizado.all { it in HEXADECIMAL }
    }
}