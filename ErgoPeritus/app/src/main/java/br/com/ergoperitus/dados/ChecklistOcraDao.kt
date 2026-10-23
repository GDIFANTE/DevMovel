package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class ChecklistOcraDao {

    @Query(
        """
        SELECT c.*
        FROM checklists_ocra AS c
        INNER JOIN atividades AS a ON a.id = c.atividade_id
        INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
        INNER JOIN eventos AS e ON e.id = f.evento_id
        WHERE c.atividade_id = :atividadeId
          AND e.usuario_id = :usuarioId
        LIMIT 1
        """
    )
    abstract suspend fun buscarPorAtividade(
        atividadeId: Long,
        usuarioId: Long
    ): ChecklistOcra?

    @Query(
        """
        SELECT EXISTS (
            SELECT 1
            FROM atividades AS a
            INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
            INNER JOIN eventos AS e ON e.id = f.evento_id
            WHERE a.id = :atividadeId
              AND e.usuario_id = :usuarioId
        )
        """
    )
    protected abstract suspend fun atividadePertenceAoUsuario(
        atividadeId: Long,
        usuarioId: Long
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun inserir(
        checklist: ChecklistOcra
    )

    @Update
    protected abstract suspend fun atualizar(
        checklist: ChecklistOcra
    ): Int

    @Transaction
    open suspend fun salvar(
        checklist: ChecklistOcra,
        usuarioId: Long
    ) {
        require(
            atividadePertenceAoUsuario(
                checklist.atividadeId,
                usuarioId
            )
        ) {
            "Atividade não encontrada para este usuário."
        }

        validar(checklist)

        val atualizado = checklist.copy(
            atualizadoEm = System.currentTimeMillis()
        )

        val existente = buscarPorAtividade(
            atividadeId = checklist.atividadeId,
            usuarioId = usuarioId
        )

        if (existente == null) {
            inserir(atualizado)
        } else {
            check(atualizar(atualizado) == 1) {
                "Não foi possível atualizar o checklist."
            }
        }
    }

    private fun validar(checklist: ChecklistOcra) {
        require(
            checklist.recuperacao == null ||
                    checklist.recuperacao in setOf(0, 2, 3, 4, 6, 10)
        ) {
            "Selecione uma alternativa válida de recuperação."
        }

        require(
            checklist.jornadaMinutos == null ||
                    checklist.jornadaMinutos in 1..1440
        ) {
            "A jornada deve estar entre 1 e 1440 minutos."
        }

        validarMedicao(
            checklist.contagemDireita,
            checklist.tempoDireitaMs,
            checklist.medicaoDireitaConcluida
        )

        validarMedicao(
            checklist.contagemEsquerda,
            checklist.tempoEsquerdaMs,
            checklist.medicaoEsquerdaConcluida
        )

        val dinamicas = setOf(
            0.0, 1.0, 3.0, 4.0, 6.0, 8.0, 10.0
        )

        require(
            checklist.frequenciaDinamicaDireita == null ||
                    checklist.frequenciaDinamicaDireita in dinamicas
        ) {
            "Frequência dinâmica direita inválida."
        }

        require(
            checklist.frequenciaDinamicaEsquerda == null ||
                    checklist.frequenciaDinamicaEsquerda in dinamicas
        ) {
            "Frequência dinâmica esquerda inválida."
        }

        val estaticas = setOf(0.0, 2.5, 4.5)

        require(
            checklist.frequenciaEstaticaDireita == null ||
                    checklist.frequenciaEstaticaDireita in estaticas
        ) {
            "Frequência estática direita inválida."
        }

        require(
            checklist.frequenciaEstaticaEsquerda == null ||
                    checklist.frequenciaEstaticaEsquerda in estaticas
        ) {
            "Frequência estática esquerda inválida."
        }
    }

    private fun validarMedicao(
        contagem: Int,
        tempoMs: Long,
        concluida: Boolean
    ) {
        require(contagem >= 0) {
            "A contagem não pode ser negativa."
        }

        require(tempoMs in 0L..60_000L) {
            "O tempo de observação deve estar entre 0 e 60 segundos."
        }

        require(!concluida || tempoMs == 60_000L) {
            "Uma medição concluída deve ter 60 segundos."
        }
    }
}