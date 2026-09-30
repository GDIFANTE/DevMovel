package br.com.ergoperitus.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EventoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirEvento(evento: Evento): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirFuncoes(funcoes: List<FuncaoEvento>)

    @Transaction
    suspend fun salvarComFuncoes(
        evento: Evento,
        nomesFuncoes: List<String>
    ): Long {
        val nomes = nomesFuncoes.map { it.trim() }

        require(nomes.isNotEmpty() && nomes.all { it.isNotEmpty() }) {
            "Informe pelo menos uma função e preencha as funções adicionadas."
        }

        val eventoId = inserirEvento(evento)

        val funcoes = nomes.mapIndexed { indice, nome ->
            FuncaoEvento(
                eventoId = eventoId,
                nome = nome,
                ordem = indice + 1
            )
        }

        inserirFuncoes(funcoes)

        return eventoId
    }

    @Query(
        """
        SELECT * FROM eventos
        WHERE usuario_id = :usuarioId
        ORDER BY criado_em DESC, id DESC
        """
    )
    suspend fun listarPorUsuario(usuarioId: Long): List<Evento>

    @Query(
        """
        SELECT * FROM eventos
        WHERE id = :eventoId AND usuario_id = :usuarioId
        LIMIT 1
        """
    )
    suspend fun buscarPorId(
        eventoId: Long,
        usuarioId: Long
    ): Evento?

    @Query(
        """
        SELECT * FROM funcoes_evento
        WHERE evento_id = :eventoId
          AND EXISTS (
              SELECT 1 FROM eventos
              WHERE id = :eventoId AND usuario_id = :usuarioId
          )
        ORDER BY ordem ASC, id ASC
        """
    )
    suspend fun listarFuncoes(
        eventoId: Long,
        usuarioId: Long
    ): List<FuncaoEvento>
}