package br.com.ergoperitus.dados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cada objeto desta classe representa um usuário salvo no celular.
 *
 * A senha nunca é guardada diretamente. O banco recebe apenas o resultado
 * matemático do hash e um salt aleatório, que serão usados na conferência
 * durante o login.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val email: String,

    @ColumnInfo(name = "senha_hash")
    val senhaHash: String,

    @ColumnInfo(name = "senha_salt")
    val senhaSalt: String,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)
