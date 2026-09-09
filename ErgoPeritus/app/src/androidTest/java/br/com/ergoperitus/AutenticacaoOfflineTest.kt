package br.com.ergoperitus

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.dados.Usuario
import br.com.ergoperitus.seguranca.ProtecaoSenha
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutenticacaoOfflineTest {

    private lateinit var banco: ErgoPeritusDatabase

    @Before
    fun prepararBanco() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()

        // O banco fica apenas na memória e desaparece ao final do teste.
        banco = Room.inMemoryDatabaseBuilder(
            contexto,
            ErgoPeritusDatabase::class.java
        ).build()
    }

    @After
    fun fecharBanco() {
        banco.close()
    }

    @Test
    fun senhaNaoEhSalvaComoTextoELoginPodeSerConferido() {
        val senhaOriginal = "Teste@123"
        val protegida = ProtecaoSenha.proteger(senhaOriginal.toCharArray())

        assertNotEquals(senhaOriginal, protegida.hash)
        assertTrue(
            ProtecaoSenha.corresponde(
                "Teste@123".toCharArray(),
                protegida.hash,
                protegida.salt
            )
        )
        assertFalse(
            ProtecaoSenha.corresponde(
                "senha-errada".toCharArray(),
                protegida.hash,
                protegida.salt
            )
        )
    }

    @Test
    fun bancoRecusaDoisUsuariosComMesmoEmail() = runBlocking {
        val protegida = ProtecaoSenha.proteger("Teste@123".toCharArray())
        val dao = banco.usuarioDao()

        val primeiroUsuario = Usuario(
            nome = "Primeiro usuário",
            email = "teste@email.com",
            senhaHash = protegida.hash,
            senhaSalt = protegida.salt
        )

        dao.inserir(primeiroUsuario)

        var emailDuplicadoFoiRecusado = false

        try {
            dao.inserir(
                primeiroUsuario.copy(
                    id = 0,
                    nome = "Segundo usuário"
                )
            )
        } catch (_: SQLiteConstraintException) {
            emailDuplicadoFoiRecusado = true
        }

        assertTrue(emailDuplicadoFoiRecusado)
    }
}
