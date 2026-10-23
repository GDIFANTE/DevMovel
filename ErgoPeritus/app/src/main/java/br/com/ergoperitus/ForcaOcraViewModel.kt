package br.com.ergoperitus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.dados.ForcaOcra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ForcaOcraViewModel(
    application: Application
) : AndroidViewModel(application) {

    private data class Pedido(
        val forca: ForcaOcra,
        val usuarioId: Long,
        val revisao: Long,
        val resposta: CompletableDeferred<Boolean>? = null
    )

    private val banco = ErgoPeritusDatabase.obter(application)
    private val fila = Channel<Pedido>(Channel.UNLIMITED)

    val registros = MutableLiveData<Map<String, ForcaOcra>>()
    val status = MutableLiveData("Carregando...")

    private var usuarioId: Long? = null
    private var atividadeId: Long? = null

    private val revisoes = mutableMapOf(
        "D" to 0L,
        "E" to 0L
    )

    private val pendentes = mutableSetOf<String>()
    private val falhas = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            for (pedido in fila) {
                try {
                    banco.forcaOcraDao().salvar(
                        pedido.forca,
                        pedido.usuarioId
                    )

                    if (
                        revisoes[pedido.forca.lado] ==
                        pedido.revisao
                    ) {
                        pendentes.remove(pedido.forca.lado)
                        falhas.remove(pedido.forca.lado)
                    }

                    atualizarStatus()
                    pedido.resposta?.complete(true)

                } catch (erro: CancellationException) {
                    pedido.resposta?.cancel()
                    throw erro

                } catch (_: Exception) {
                    if (
                        revisoes[pedido.forca.lado] ==
                        pedido.revisao
                    ) {
                        pendentes.remove(pedido.forca.lado)
                        falhas.add(pedido.forca.lado)
                    }

                    atualizarStatus()
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

        val direito = banco.forcaOcraDao().buscar(
            idAtividade,
            "D",
            idUsuario
        ) ?: ForcaOcra(idAtividade, "D")

        val esquerdo = banco.forcaOcraDao().buscar(
            idAtividade,
            "E",
            idUsuario
        ) ?: ForcaOcra(idAtividade, "E")

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
        transformar: (ForcaOcra) -> ForcaOcra
    ) {
        val mapa = registros.value ?: return
        val anterior = mapa[lado] ?: return

        var novo = transformar(anterior)

        // Mantém o registro vinculado à atividade e ao lado originais.
        novo = novo.copy(
            atividadeId = anterior.atividadeId,
            lado = anterior.lado
        )

        if (novo.haForcaRepetida != true) {
            novo = novo.copy(
                borgModerada = null,
                tempoModerada = null,
                borgForte = null,
                tempoForte = null,
                borgQuaseMaxima = null,
                tempoQuaseMaxima = null
            )
        }

        if (novo == anterior) return

        registros.value = mapa + (lado to novo)
        enfileirar(novo)
    }

    private fun enfileirar(
        forca: ForcaOcra,
        resposta: CompletableDeferred<Boolean>? = null
    ) {
        val id = usuarioId ?: return

        val revisao = (revisoes[forca.lado] ?: 0L) + 1L
        revisoes[forca.lado] = revisao

        pendentes.add(forca.lado)
        falhas.remove(forca.lado)
        atualizarStatus()

        fila.trySend(
            Pedido(
                forca,
                id,
                revisao,
                resposta
            )
        )
    }

    private fun atualizarStatus() {
        status.value = when {
            falhas.isNotEmpty() ->
                "Falha ao salvar. Tente salvar novamente."

            pendentes.isNotEmpty() ->
                "Salvando..."

            else ->
                "Salvo"
        }
    }

    suspend fun salvarAgora(): Boolean {
        val mapa = registros.value ?: return false

        val direito = CompletableDeferred<Boolean>()
        val esquerdo = CompletableDeferred<Boolean>()

        enfileirar(mapa.getValue("D"), direito)
        enfileirar(mapa.getValue("E"), esquerdo)

        val salvouDireito = direito.await()
        val salvouEsquerdo = esquerdo.await()

        return salvouDireito && salvouEsquerdo
    }
}