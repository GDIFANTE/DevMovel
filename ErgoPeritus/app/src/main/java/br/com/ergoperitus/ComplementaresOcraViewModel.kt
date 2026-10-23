package br.com.ergoperitus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import br.com.ergoperitus.dados.ComplementaresOcra
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ComplementaresOcraViewModel(
    application: Application
) : AndroidViewModel(application) {

    private data class Pedido(
        val registros: Map<String, ComplementaresOcra>,
        val usuarioId: Long,
        val revisao: Long,
        val resposta: CompletableDeferred<Boolean>? = null
    )

    private val banco = ErgoPeritusDatabase.obter(application)
    private val fila = Channel<Pedido>(Channel.UNLIMITED)

    val registros = MutableLiveData<Map<String, ComplementaresOcra>>()
    val status = MutableLiveData("Carregando...")

    var ladoSelecionado = "D"

    private var usuarioId: Long? = null
    private var atividadeId: Long? = null
    private var revisaoAtual = 0L

    init {
        viewModelScope.launch {
            for (pedido in fila) {
                try {
                    banco.withTransaction {
                        pedido.registros.values.forEach {
                            banco.complementaresOcraDao().salvar(
                                it,
                                pedido.usuarioId
                            )
                        }
                    }

                    if (pedido.revisao == revisaoAtual) {
                        status.value = "Salvo"
                    }

                    pedido.resposta?.complete(true)

                } catch (erro: CancellationException) {
                    pedido.resposta?.cancel()
                    throw erro

                } catch (_: Exception) {
                    if (pedido.revisao == revisaoAtual) {
                        status.value =
                            "Falha ao salvar. Tente salvar novamente."
                    }

                    pedido.resposta?.complete(false)
                }
            }
        }
    }

    suspend fun carregar(
        idAtividade: Long,
        idUsuario: Long
    ) {
        if (atividadeId != null) {
            check(
                atividadeId == idAtividade &&
                        usuarioId == idUsuario
            )
            return
        }

        check(
            banco.atividadeDao().buscarPorId(
                idAtividade,
                idUsuario
            ) != null
        ) {
            "Atividade não encontrada."
        }

        val direito = banco.complementaresOcraDao().buscar(
            idAtividade,
            "D",
            idUsuario
        ) ?: ComplementaresOcra(idAtividade, "D")

        val esquerdo = banco.complementaresOcraDao().buscar(
            idAtividade,
            "E",
            idUsuario
        ) ?: ComplementaresOcra(idAtividade, "E")

        atividadeId = idAtividade
        usuarioId = idUsuario

        registros.value = mapOf(
            "D" to direito,
            "E" to esquerdo
        )

        status.value = "Pronto"
    }

    fun alterar(
        lado: String,
        transformar: (ComplementaresOcra) -> ComplementaresOcra
    ) {
        val mapa = registros.value ?: return
        val anterior = mapa[lado] ?: return

        val novo = transformar(anterior).copy(
            atividadeId = anterior.atividadeId,
            lado = anterior.lado
        )

        if (novo == anterior) return

        registros.value = mapa + (lado to novo)
        enfileirar()
    }

    fun copiarParaOutroBraco(ladoOrigem: String) {
        val mapa = registros.value ?: return
        val origem = mapa[ladoOrigem] ?: return

        val destino = if (ladoOrigem == "D") "E" else "D"

        registros.value = mapa + (
                destino to origem.copy(lado = destino)
                )

        enfileirar()
    }

    private fun enfileirar(
        resposta: CompletableDeferred<Boolean>? = null
    ) {
        val mapa = registros.value ?: return
        val id = usuarioId ?: return

        revisaoAtual++
        status.value = "Salvando..."

        fila.trySend(
            Pedido(
                mapa.toMap(),
                id,
                revisaoAtual,
                resposta
            )
        )
    }

    suspend fun salvarAgora(): Boolean {
        if (registros.value == null || usuarioId == null) {
            return false
        }

        val resposta = CompletableDeferred<Boolean>()
        enfileirar(resposta)

        return resposta.await()
    }
}