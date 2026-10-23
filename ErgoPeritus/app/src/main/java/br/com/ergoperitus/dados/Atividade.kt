package br.com.ergoperitus.dados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "atividades",
    foreignKeys = [
        ForeignKey(
            entity = FuncaoEvento::class,
            parentColumns = ["id"],
            childColumns = ["funcao_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["funcao_id"])
    ]
)
data class Atividade(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "funcao_id")
    val funcaoId: Long,

    val nome: String = "",

    val descricao: String = "",

    @ColumnInfo(name = "duracao_diaria_minutos")
    val duracaoDiariaMinutos: Int? = null,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long = System.currentTimeMillis()
)