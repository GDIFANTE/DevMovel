package br.com.ergoperitus.seguranca

import android.content.Context

/**
 * O banco guarda os dados permanentes do usuário. Esta classe guarda somente
 * o pequeno estado necessário para saber qual conta poderá usar a biometria.
 */
class SessaoUsuario(context: Context) {

    private val preferencias = context.getSharedPreferences(
        ARQUIVO_PREFERENCIAS,
        Context.MODE_PRIVATE
    )

    fun registrarLogin(usuarioId: Long) {
        preferencias.edit()
            .putLong(CHAVE_USUARIO_ID, usuarioId)
            .apply()
    }

    fun usuarioId(): Long? {
        val id = preferencias.getLong(CHAVE_USUARIO_ID, SEM_USUARIO)
        return id.takeIf { it != SEM_USUARIO }
    }

    fun biometriaAtivada(): Boolean {
        return preferencias.getBoolean(CHAVE_BIOMETRIA, false)
    }

    fun ativarBiometria() {
        preferencias.edit()
            .putBoolean(CHAVE_BIOMETRIA, true)
            .apply()
    }

    /**
     * Sair remove o vínculo local da conta e também desativa a biometria.
     * O cadastro continua no banco e poderá ser usado no próximo login.
     */
    fun encerrar() {
        preferencias.edit().clear().apply()
    }

    companion object {
        private const val ARQUIVO_PREFERENCIAS = "sessao_ergo_peritus"
        private const val CHAVE_USUARIO_ID = "usuario_id"
        private const val CHAVE_BIOMETRIA = "biometria_ativada"
        private const val SEM_USUARIO = -1L
    }
}
