package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class PosturaOcraDao {

    @Query(
        """
        SELECT p.*
        FROM postura_ocra AS p
        INNER JOIN atividades AS a ON a.id = p.atividade_id
        INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
        INNER JOIN eventos AS e ON e.id = f.evento_id
        WHERE p.atividade_id = :atividadeId
          AND p.lado = :lado
          AND e.usuario_id = :usuarioId
        LIMIT 1
        """
    )
    abstract suspend fun buscar(
        atividadeId: Long,
        lado: String,
        usuarioId: Long
    ): PosturaOcra?

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
    protected abstract suspend fun inserir(postura: PosturaOcra)

    @Update
    protected abstract suspend fun atualizar(postura: PosturaOcra): Int

    @Transaction
    open suspend fun salvar(
        postura: PosturaOcra,
        usuarioId: Long
    ) {
        validar(postura)

        require(
            pertenceAoUsuario(postura.atividadeId, usuarioId)
        ) {
            "Atividade não encontrada para este usuário."
        }

        gravar(postura, usuarioId)
    }

    @Transaction
    open suspend fun salvarECopiarParaOutroBraco(
        postura: PosturaOcra,
        usuarioId: Long
    ) {
        validar(postura)

        require(
            pertenceAoUsuario(postura.atividadeId, usuarioId)
        ) {
            "Atividade não encontrada para este usuário."
        }

        val outroLado = if (postura.lado == "D") {
            "E"
        } else {
            "D"
        }

        gravar(postura, usuarioId)

        gravar(
            postura.copy(lado = outroLado),
            usuarioId
        )
    }

    private suspend fun gravar(
        postura: PosturaOcra,
        usuarioId: Long
    ) {
        val atualizada = postura.copy(
            atualizadoEm = System.currentTimeMillis()
        )

        val existente = buscar(
            postura.atividadeId,
            postura.lado,
            usuarioId
        )

        if (existente == null) {
            inserir(atualizada)
        } else {
            check(atualizar(atualizada) == 1) {
                "Não foi possível salvar a avaliação de postura."
            }
        }
    }

    private fun validar(postura: PosturaOcra) {
        require(postura.lado == "D" || postura.lado == "E") {
            "Lado inválido."
        }

        require(
            postura.ombro == null ||
                    postura.ombro in setOf(0, 1, 2, 6, 12, 24)
        ) {
            "Pontuação de ombro inválida."
        }

        val opcoesArticulares = setOf(0, 2, 4, 8)

        require(
            postura.cotovelo == null ||
                    postura.cotovelo in opcoesArticulares
        ) {
            "Pontuação de cotovelo inválida."
        }

        require(
            postura.punho == null ||
                    postura.punho in opcoesArticulares
        ) {
            "Pontuação de punho inválida."
        }

        require(
            postura.maoDedos == null ||
                    postura.maoDedos in opcoesArticulares
        ) {
            "Pontuação de mão e dedos inválida."
        }

        require(
            postura.preensoes == null ||
                    postura.preensoes in 0..15
        ) {
            "Tipos de preensão inválidos."
        }

        require(
            postura.estereotipia == null ||
                    postura.estereotipia in setOf(0.0, 1.5, 3.0)
        ) {
            "Pontuação de estereotipia inválida."
        }
    }
}