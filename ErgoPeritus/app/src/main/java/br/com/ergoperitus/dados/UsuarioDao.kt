package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UsuarioDao {

    /**
     * O LIMIT 1 deixa clara a intenção da consulta: um e-mail identifica
     * somente uma conta dentro do aplicativo.
     */
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): Usuario?

    /**
     * ABORT impede que um segundo cadastro sobrescreva silenciosamente uma
     * conta que já utiliza o mesmo e-mail.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(usuario: Usuario): Long

    /**
     * Atualiza o usuário identificado pelo seu ID,
     * mantendo o mesmo cadastro no banco.
     */
    @Update
    suspend fun atualizar(usuario: Usuario): Int

    /**
     * Redefine a senha somente se o código conferido ainda estiver ativo.
     * Na mesma operação, remove o código para impedir sua reutilização.
     */
    @Query(
        """
        UPDATE usuarios
        SET senha_hash = :novaSenhaHash,
            senha_salt = :novaSenhaSalt,
            recuperacao_hash = NULL,
            recuperacao_salt = NULL
        WHERE id = :usuarioId
          AND recuperacao_hash = :codigoHash
          AND recuperacao_salt = :codigoSalt
        """
    )
    suspend fun redefinirSenhaComCodigo(
        usuarioId: Long,
        codigoHash: String,
        codigoSalt: String,
        novaSenhaHash: String,
        novaSenhaSalt: String
    ): Int
}
