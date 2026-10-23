package br.com.ergoperitus

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import br.com.ergoperitus.dados.Atividade
import br.com.ergoperitus.dados.ChecklistOcra
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChecklistOcraViewModel(
    application: Application
) : AndroidViewModel(application) {

    enum class Lado {
        DIREITO,
        ESQUERDO
    }

    data class Formulario(
        val atividade: Atividade,
        val checklist: ChecklistOcra
    )

    private data class PedidoSalvamento(
        val formulario: Formulario,
        val usuarioId: Long,
        val revisao: Long,
        val resposta: CompletableDeferred<Boolean>? = null
    )

    private val banco = ErgoPeritusDatabase.obter(application)

    val status = MutableLiveData("Carregando...")
    val checklistVisivel = MutableLiveData<ChecklistOcra>()
    val ladoEmMedicao = MutableLiveData<Lado?>(null)

    private val fila = Channel<PedidoSalvamento>(Channel.UNLIMITED)

    private var formularioAtual: Formulario? = null
    private var usuarioCarregado: Long? = null
    private var revisaoAtual = 0L

    private var cronometro: Job? = null
    private var inicioMedicaoMs = 0L
    private var ultimoSegundoSalvo = -1L

    init {
        viewModelScope.launch {
            for (pedido in fila) {
                try {
                    banco.withTransaction {
                        banco.atividadeDao().salvarDados(
                            atividadeId = pedido.formulario.atividade.id,
                            usuarioId = pedido.usuarioId,
                            nome = pedido.formulario.atividade.nome,
                            descricao = pedido.formulario.atividade.descricao,
                            duracaoDiariaMinutos =
                                pedido.formulario.atividade.duracaoDiariaMinutos
                        )

                        banco.checklistOcraDao().salvar(
                            checklist = pedido.formulario.checklist,
                            usuarioId = pedido.usuarioId
                        )
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
                            "Falha ao salvar. Toque em Tentar salvar novamente."
                    }

                    pedido.resposta?.complete(false)
                }
            }
        }
    }

    suspend fun carregar(
        atividadeId: Long,
        usuarioId: Long
    ): Formulario {
        formularioAtual?.let {
            check(
                it.atividade.id == atividadeId &&
                        usuarioCarregado == usuarioId
            ) {
                "O formulário pertence a outra atividade."
            }

            return it
        }

        val atividade = banco.atividadeDao().buscarPorId(
            atividadeId,
            usuarioId
        ) ?: error("Atividade não encontrada.")

        val checklist = banco.checklistOcraDao()
            .buscarPorAtividade(atividadeId, usuarioId)
            ?: ChecklistOcra(atividadeId = atividadeId)

        val formulario = Formulario(atividade, checklist)

        usuarioCarregado = usuarioId
        formularioAtual = formulario

        checklistVisivel.value = checklist
        status.value = "Pronto"

        return formulario
    }

    fun atualizar(
        nome: String,
        descricao: String,
        duracaoMinutos: Int?,
        jornadaMinutos: Int?,
        recuperacao: Int?,
        distribuicaoPausas: String
    ) {
        val atual = formularioAtual ?: return

        val novo = atual.copy(
            atividade = atual.atividade.copy(
                nome = nome,
                descricao = descricao,
                duracaoDiariaMinutos = duracaoMinutos
            ),
            checklist = atual.checklist.copy(
                jornadaMinutos = jornadaMinutos,
                recuperacao = recuperacao,
                distribuicaoPausas = distribuicaoPausas
            )
        )

        if (novo == atual) return

        formularioAtual = novo
        enfileirarSalvamento()
    }

    private fun alterarChecklist(
        novo: ChecklistOcra,
        persistir: Boolean = true
    ) {
        val atual = formularioAtual ?: return

        formularioAtual = atual.copy(checklist = novo)
        checklistVisivel.value = novo

        if (persistir) {
            enfileirarSalvamento()
        }
    }

    private fun enfileirarSalvamento() {
        val formulario = formularioAtual ?: return
        val usuarioId = usuarioCarregado ?: return

        revisaoAtual++
        status.value = "Salvando..."

        fila.trySend(
            PedidoSalvamento(
                formulario,
                usuarioId,
                revisaoAtual
            )
        )
    }

    fun iniciarMedicao(lado: Lado) {
        if (ladoEmMedicao.value != null) return

        val atual = formularioAtual?.checklist ?: return

        val novo = when (lado) {
            Lado.DIREITO -> atual.copy(
                contagemDireita = 0,
                tempoDireitaMs = 0,
                medicaoDireitaConcluida = false,
                frequenciaDinamicaDireita = null
            )

            Lado.ESQUERDO -> atual.copy(
                contagemEsquerda = 0,
                tempoEsquerdaMs = 0,
                medicaoEsquerdaConcluida = false,
                frequenciaDinamicaEsquerda = null
            )
        }

        inicioMedicaoMs = SystemClock.elapsedRealtime()
        ultimoSegundoSalvo = 0L

        ladoEmMedicao.value = lado
        alterarChecklist(novo)

        cronometro = viewModelScope.launch {
            while (ladoEmMedicao.value == lado) {
                delay(100)

                if (ladoEmMedicao.value == lado) {
                    atualizarRelogio()
                }
            }
        }
    }

    private fun atualizarRelogio(): Boolean {
        val lado = ladoEmMedicao.value ?: return false
        val atual = formularioAtual?.checklist ?: return false

        val decorrido = (
                SystemClock.elapsedRealtime() - inicioMedicaoMs
                ).coerceIn(0L, 60_000L)

        val concluida = decorrido >= 60_000L
        val segundo = decorrido / 1000L

        val persistir =
            concluida || segundo != ultimoSegundoSalvo

        if (persistir) {
            ultimoSegundoSalvo = segundo
        }

        val novo = when (lado) {
            Lado.DIREITO -> atual.copy(
                tempoDireitaMs = decorrido,
                medicaoDireitaConcluida = concluida
            )

            Lado.ESQUERDO -> atual.copy(
                tempoEsquerdaMs = decorrido,
                medicaoEsquerdaConcluida = concluida
            )
        }

        if (concluida) {
            ladoEmMedicao.value = null
        }

        alterarChecklist(novo, persistir)

        return !concluida
    }

    fun contar(lado: Lado, incremento: Int) {
        if (ladoEmMedicao.value != lado) return

        // Confere o tempo também no toque para impedir
        // contagens feitas depois do limite de 60 segundos.
        if (!atualizarRelogio()) return

        val atual = formularioAtual?.checklist ?: return

        val novo = when (lado) {
            Lado.DIREITO -> atual.copy(
                contagemDireita = (
                        atual.contagemDireita + incremento
                        ).coerceAtLeast(0)
            )

            Lado.ESQUERDO -> atual.copy(
                contagemEsquerda = (
                        atual.contagemEsquerda + incremento
                        ).coerceAtLeast(0)
            )
        }

        alterarChecklist(novo)
    }

    fun interromperMedicao() {
        if (ladoEmMedicao.value == null) return

        atualizarRelogio()

        ladoEmMedicao.value = null
        cronometro?.cancel()
        cronometro = null

        // Grava o instante final da observação interrompida.
        enfileirarSalvamento()
    }

    fun selecionarFrequencia(
        lado: Lado,
        estatica: Boolean,
        pontos: Double?
    ) {
        val atual = formularioAtual?.checklist ?: return

        val novo = when {
            lado == Lado.DIREITO && estatica ->
                atual.copy(frequenciaEstaticaDireita = pontos)

            lado == Lado.DIREITO ->
                atual.copy(frequenciaDinamicaDireita = pontos)

            estatica ->
                atual.copy(frequenciaEstaticaEsquerda = pontos)

            else ->
                atual.copy(frequenciaDinamicaEsquerda = pontos)
        }

        alterarChecklist(novo)
    }

    suspend fun salvarAgora(): Boolean {
        val formulario = formularioAtual ?: return false
        val usuarioId = usuarioCarregado ?: return false

        val resposta = CompletableDeferred<Boolean>()

        status.value = "Salvando..."

        fila.send(
            PedidoSalvamento(
                formulario,
                usuarioId,
                revisaoAtual,
                resposta
            )
        )

        return resposta.await()
    }
}