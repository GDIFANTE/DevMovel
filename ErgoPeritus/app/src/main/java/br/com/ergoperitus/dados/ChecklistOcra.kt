package br.com.ergoperitus.dados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklists_ocra",
    foreignKeys = [
        ForeignKey(
            entity = Atividade::class,
            parentColumns = ["id"],
            childColumns = ["atividade_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChecklistOcra(
    // Um checklist por atividade.
    @PrimaryKey
    @ColumnInfo(name = "atividade_id")
    val atividadeId: Long,

    // Pontuação da alternativa selecionada no campo de pausas.
    @ColumnInfo(name = "recuperacao")
    val recuperacao: Int? = null,

    @ColumnInfo(name = "jornada_minutos")
    val jornadaMinutos: Int? = null,

    @ColumnInfo(name = "distribuicao_pausas")
    val distribuicaoPausas: String = "",

    // Contagem do braço direito.
    @ColumnInfo(name = "contagem_direita")
    val contagemDireita: Int = 0,

    @ColumnInfo(name = "tempo_direita_ms")
    val tempoDireitaMs: Long = 0,

    @ColumnInfo(name = "medicao_direita_concluida")
    val medicaoDireitaConcluida: Boolean = false,

    // Contagem do braço esquerdo.
    @ColumnInfo(name = "contagem_esquerda")
    val contagemEsquerda: Int = 0,

    @ColumnInfo(name = "tempo_esquerda_ms")
    val tempoEsquerdaMs: Long = 0,

    @ColumnInfo(name = "medicao_esquerda_concluida")
    val medicaoEsquerdaConcluida: Boolean = false,

    // Alternativas escolhidas pela perita, considerando o ritmo
    // e a possibilidade de interrupções, além da contagem.
    @ColumnInfo(name = "frequencia_dinamica_direita")
    val frequenciaDinamicaDireita: Double? = null,

    @ColumnInfo(name = "frequencia_dinamica_esquerda")
    val frequenciaDinamicaEsquerda: Double? = null,

    // Zero significa ausência da condição estática.
    // Null significa que a pergunta ainda não foi respondida.
    @ColumnInfo(name = "frequencia_estatica_direita")
    val frequenciaEstaticaDireita: Double? = null,

    @ColumnInfo(name = "frequencia_estatica_esquerda")
    val frequenciaEstaticaEsquerda: Double? = null,

    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long = System.currentTimeMillis()
)