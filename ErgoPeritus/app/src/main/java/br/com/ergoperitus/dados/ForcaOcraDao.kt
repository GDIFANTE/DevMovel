package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class ForcaOcraDao {

    @Query(
        """
        SELECT r.*
        FROM forca_ocra AS r
        INNER JOIN atividades AS a ON a.id = r.atividade_id
        INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
        INNER JOIN eventos AS e ON e.id = f.evento_id
        WHERE r.atividade_id = :atividadeId
          AND r.lado = :lado
          AND e.usuario_id = :usuarioId
        LIMIT 1
        """
    )
    abstract suspend fun buscar(
        atividadeId: Long,
        lado: String,
        usuarioId: Long
    ): ForcaOcra?

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
    protected abstract suspend fun pertenceAoUsuario(
        atividadeId: Long,
        usuarioId: Long
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun inserir(forca: ForcaOcra)

    @Update
    protected abstract suspend fun atualizar(forca: ForcaOcra): Int

    @Transaction
    open suspend fun salvar(
        forca: ForcaOcra,
        usuarioId: Long
    ) {
        require(forca.lado == "D" || forca.lado == "E") {
            "Lado inválido."
        }

        require(
            pertenceAoUsuario(forca.atividadeId, usuarioId)
        ) {
            "Atividade não encontrada para este usuário."
        }

        validar(forca)

        // Se a resposta for Não ou Ainda não avaliado,
        // não mantém intensidades antigas como respostas ativas.
        val normalizada = if (forca.haForcaRepetida == true) {
            forca
        } else {
            forca.copy(
                borgModerada = null,
                tempoModerada = null,
                borgForte = null,
                tempoForte = null,
                borgQuaseMaxima = null,
                tempoQuaseMaxima = null
            )
        }

        val atualizada = normalizada.copy(
            atualizadoEm = System.currentTimeMillis()
        )

        val existente = buscar(
            forca.atividadeId,
            forca.lado,
            usuarioId
        )

        if (existente == null) {
            inserir(atualizada)
        } else {
            check(atualizar(atualizada) == 1) {
                "Não foi possível salvar a avaliação de força."
            }
        }
    }

    private fun validar(forca: ForcaOcra) {
        require(
            forca.borgModerada == null ||
                    forca.borgModerada in 3..4
        ) {
            "A força moderada deve corresponder a Borg 3 ou 4."
        }

        require(
            forca.borgForte == null ||
                    forca.borgForte in 5..7
        ) {
            "A força forte deve corresponder a Borg 5, 6 ou 7."
        }

        require(
            forca.borgQuaseMaxima == null ||
                    forca.borgQuaseMaxima in 8..10
        ) {
            "A força quase máxima deve corresponder a Borg 8, 9 ou 10."
        }

        val temposModerados = setOf(
            "TERCO",
            "METADE",
            "MAIS_METADE",
            "QUASE_TODO"
        )

        require(
            forca.tempoModerada == null ||
                    forca.tempoModerada in temposModerados
        ) {
            "Tempo de força moderada inválido."
        }

        val temposIntensos = setOf(
            "DOIS_SEGUNDOS",
            "UM_PORCENTO",
            "CINCO_PORCENTO",
            "MAIS_DEZ_PORCENTO"
        )

        require(
            forca.tempoForte == null ||
                    forca.tempoForte in temposIntensos
        ) {
            "Tempo de força forte inválido."
        }

        require(
            forca.tempoQuaseMaxima == null ||
                    forca.tempoQuaseMaxima in temposIntensos
        ) {
            "Tempo de força quase máxima inválido."
        }
    }
}