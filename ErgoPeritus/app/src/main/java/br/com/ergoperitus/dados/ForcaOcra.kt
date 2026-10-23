package br.com.ergoperitus.dados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "forca_ocra",
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
data class ForcaOcra(
    @ColumnInfo(name = "atividade_id")
    val atividadeId: Long,

    // D = direito; E = esquerdo.
    val lado: String,

    @ColumnInfo(name = "ha_forca_repetida")
    val haForcaRepetida: Boolean? = null,

    // Borg 3 ou 4.
    @ColumnInfo(name = "borg_moderada")
    val borgModerada: Int? = null,

    // TERCO, METADE, MAIS_METADE ou QUASE_TODO.
    @ColumnInfo(name = "tempo_moderada")
    val tempoModerada: String? = null,

    // Borg 5, 6 ou 7.
    @ColumnInfo(name = "borg_forte")
    val borgForte: Int? = null,

    // DOIS_SEGUNDOS, UM_PORCENTO, CINCO_PORCENTO
    // ou MAIS_DEZ_PORCENTO.
    @ColumnInfo(name = "tempo_forte")
    val tempoForte: String? = null,

    // Borg 8, 9 ou 10.
    @ColumnInfo(name = "borg_quase_maxima")
    val borgQuaseMaxima: Int? = null,

    @ColumnInfo(name = "tempo_quase_maxima")
    val tempoQuaseMaxima: String? = null,

    // Permite documentar as ações que exigem força.
    val observacoes: String = "",

    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long = System.currentTimeMillis()
)