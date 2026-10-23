package br.com.ergoperitus.dados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "complementares_ocra",
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
data class ComplementaresOcra(
    @ColumnInfo(name = "atividade_id")
    val atividadeId: Long,

    // D = direito; E = esquerdo.
    val lado: String,

    // Combinação dos fatores observados.
    // Null = ainda não avaliado; 0 = nenhum fator.
    val fatores: Int? = null,

    // Uma pontuação para o bloco físico: 0, 2, 3 ou 4.
    @ColumnInfo(name = "pontuacao_fisica")
    val pontuacaoFisica: Int? = null,

    // 0 = sem ritmo imposto pela máquina.
    // 1 = imposto pela máquina, com possibilidade de ajuste.
    // 2 = completamente determinado pela máquina.
    // Null = ainda não avaliado.
    @ColumnInfo(name = "ritmo_maquina")
    val ritmoMaquina: Int? = null,

    val observacoes: String = "",

    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long = System.currentTimeMillis()
) {
    companion object {
        const val LUVAS_INADEQUADAS = 1
        const val MOVIMENTOS_BRUSCOS = 2
        const val IMPACTOS = 4
        const val FRIO = 8
        const val VIBRACAO = 16
        const val COMPRESSAO = 32
        const val PRECISAO = 64
        const val OUTROS = 128
    }
}