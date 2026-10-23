package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AtividadeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun inserir(atividade: Atividade): Long

    @Query(
        """
        SELECT EXISTS (
            SELECT 1
            FROM funcoes_evento AS f
            INNER JOIN eventos AS e ON e.id = f.evento_id
            WHERE f.id = :funcaoId
              AND e.usuario_id = :usuarioId
        )
        """
    )
    protected abstract suspend fun funcaoPertenceAoUsuario(
        funcaoId: Long,
        usuarioId: Long
    ): Boolean

    @Transaction
    open suspend fun criar(
        funcaoId: Long,
        usuarioId: Long
    ): Long {
        require(funcaoPertenceAoUsuario(funcaoId, usuarioId)) {
            "Função não encontrada para este usuário."
        }

        val agora = System.currentTimeMillis()

        return inserir(
            Atividade(
                funcaoId = funcaoId,
                criadoEm = agora,
                atualizadoEm = agora
            )
        )
    }

    @Query(
        """
        SELECT a.*
        FROM atividades AS a
        INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
        INNER JOIN eventos AS e ON e.id = f.evento_id
        WHERE a.funcao_id = :funcaoId
          AND e.usuario_id = :usuarioId
        ORDER BY a.criado_em ASC, a.id ASC
        """
    )
    abstract suspend fun listarPorFuncao(
        funcaoId: Long,
        usuarioId: Long
    ): List<Atividade>

    @Query(
        """
        SELECT a.*
        FROM atividades AS a
        INNER JOIN funcoes_evento AS f ON f.id = a.funcao_id
        INNER JOIN eventos AS e ON e.id = f.evento_id
        WHERE a.id = :atividadeId
          AND e.usuario_id = :usuarioId
        LIMIT 1
        """
    )
    abstract suspend fun buscarPorId(
        atividadeId: Long,
        usuarioId: Long
    ): Atividade?

    @Query(
        """
        UPDATE atividades
        SET nome = :nome,
            descricao = :descricao,
            duracao_diaria_minutos = :duracaoDiariaMinutos,
            atualizado_em = :atualizadoEm
        WHERE id = :atividadeId
          AND EXISTS (
              SELECT 1
              FROM funcoes_evento AS f
              INNER JOIN eventos AS e ON e.id = f.evento_id
              WHERE f.id = atividades.funcao_id
                AND e.usuario_id = :usuarioId
          )
        """
    )
    protected abstract suspend fun atualizarCampos(
        atividadeId: Long,
        usuarioId: Long,
        nome: String,
        descricao: String,
        duracaoDiariaMinutos: Int?,
        atualizadoEm: Long
    ): Int

    @Transaction
    open suspend fun salvarDados(
        atividadeId: Long,
        usuarioId: Long,
        nome: String,
        descricao: String,
        duracaoDiariaMinutos: Int?
    ) {
        require(
            duracaoDiariaMinutos == null ||
                    duracaoDiariaMinutos in 1..1440
        ) {
            "A duração diária deve estar entre 1 e 1440 minutos."
        }

        val alterados = atualizarCampos(
            atividadeId = atividadeId,
            usuarioId = usuarioId,
            nome = nome,
            descricao = descricao,
            duracaoDiariaMinutos = duracaoDiariaMinutos,
            atualizadoEm = System.currentTimeMillis()
        )

        check(alterados == 1) {
            "Não foi possível salvar a atividade deste usuário."
        }
    }
}