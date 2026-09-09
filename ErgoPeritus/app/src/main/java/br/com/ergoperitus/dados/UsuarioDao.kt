package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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
}
