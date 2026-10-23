package br.com.ergoperitus.dados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "postura_ocra",
    primaryKeys = ["atividade_id", "lado"],
    foreignKeys = [
        ForeignKey(
            entity = Atividade::class,
            parentColumns = ["id"],
            childColumns = ["atividade_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PosturaOcra(
    @ColumnInfo(name = "atividade_id")
    val atividadeId: Long,

    // D = direito; E = esquerdo.
    val lado: String,

    // Pontuação da alternativa de ombro, antes da duplicação.
    // Opções: 0, 1, 2, 6, 12 e 24.
    val ombro: Int? = null,

    @ColumnInfo(name = "maos_acima_cabeca")
    val maosAcimaCabeca: Boolean? = null,

    // Opções: 0, 2, 4 e 8.
    val cotovelo: Int? = null,

    // Opções: 0, 2, 4 e 8.
    val punho: Int? = null,

    // Pontuação relacionada à duração das preensões.
    // Opções: 0, 2, 4 e 8.
    @ColumnInfo(name = "mao_dedos")
    val maoDedos: Int? = null,

    // Combinação dos tipos de preensão observados:
    // 1 = pinça; 2 = palmar; 4 = gancho; 8 = outra comparável.
    // A soma permite registrar mais de um tipo.
    // 0 = nenhum; null = ainda não avaliado.
    val preensoes: Int? = null,

    // Opções: 0, 1.5 e 3.
    val estereotipia: Double? = null,

    val observacoes: String = "",

    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long = System.currentTimeMillis()
)