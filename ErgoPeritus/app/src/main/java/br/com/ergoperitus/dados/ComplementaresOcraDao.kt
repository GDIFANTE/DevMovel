package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class ComplementaresOcraDao {

    @Query(
        """
        SELECT c.*
        FROM complementares_ocra AS c
        INNER JOIN atividades AS a ON a.id = c.atividade_id
        INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
        INNER JOIN eventos AS e ON e.id = f.evento_id
        WHERE c.atividade_id = :atividadeId
          AND c.lado = :lado
          AND e.usuario_id = :usuarioId
        LIMIT 1
        """
    )
    abstract suspend fun buscar(
        atividadeId: Long,
        lado: String,
        usuarioId: Long
    ): ComplementaresOcra?

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
    protected abstract suspend fun inserir(
        complementares: ComplementaresOcra
    )

    @Update
    protected abstract suspend fun atualizar(
        complementares: ComplementaresOcra
    ): Int

    @Transaction
    open suspend fun salvar(
        complementares: ComplementaresOcra,
        usuarioId: Long
    ) {
        require(
            complementares.lado == "D" ||
                    complementares.lado == "E"
        ) {
            "Lado inválido."
        }

        require(
            pertenceAoUsuario(
                complementares.atividadeId,
                usuarioId
            )
        ) {
            "Atividade não encontrada para este usuário."
        }

        require(
            complementares.fatores == null ||
                    complementares.fatores in 0..255
        ) {
            "Fatores complementares inválidos."
        }

        require(
            complementares.pontuacaoFisica == null ||
                    complementares.pontuacaoFisica in setOf(0, 2, 3, 4)
        ) {
            "Pontuação do bloco físico inválida."
        }

        require(
            complementares.ritmoMaquina == null ||
                    complementares.ritmoMaquina in 0..2
        ) {
            "Pontuação de ritmo da máquina inválida."
        }

        val atualizado = complementares.copy(
            atualizadoEm = System.currentTimeMillis()
        )

        val existente = buscar(
            complementares.atividadeId,
            complementares.lado,
            usuarioId
        )

        if (existente == null) {
            inserir(atualizado)
        } else {
            check(atualizar(atualizado) == 1) {
                "Não foi possível salvar os fatores complementares."
            }
        }
    }
}