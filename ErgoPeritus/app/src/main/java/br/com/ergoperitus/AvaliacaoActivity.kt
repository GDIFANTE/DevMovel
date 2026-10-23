package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.Atividade
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.dados.FuncaoEvento
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AvaliacaoActivity : AppCompatActivity() {

    private val banco by lazy {
        ErgoPeritusDatabase.obter(applicationContext)
    }

    private val sessao by lazy {
        SessaoUsuario(applicationContext)
    }

    private lateinit var conteudo: LinearLayout

    private var eventoId = 0L
    private var usuarioId = 0L
    private var funcaoSelecionadaId: Long? = null
    private var carregando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        eventoId = intent.getLongExtra(EXTRA_EVENTO_ID, 0L)

        val id = sessao.usuarioId()

        if (id == null) {
            voltarAoLogin()
            return
        }

        usuarioId = id

        if (eventoId <= 0L) {
            mostrarMensagem("Avaliação não encontrada.")
            finish()
            return
        }

        funcaoSelecionadaId = savedInstanceState
            ?.getLong(CHAVE_FUNCAO)
            ?.takeIf { it > 0L }

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(conteudo)
            }
        )
    }

    override fun onResume() {
        super.onResume()

        if (::conteudo.isInitialized && usuarioId > 0L) {
            carregarAvaliacao()
        }
    }

    private fun carregarAvaliacao() {
        if (carregando) return
        carregando = true

        lifecycleScope.launch {
            try {
                val evento = banco.eventoDao().buscarPorId(
                    eventoId,
                    usuarioId
                )

                if (evento == null) {
                    mostrarMensagem(
                        "Avaliação não encontrada para este usuário."
                    )
                    finish()
                    return@launch
                }

                val funcoes = banco.eventoDao().listarFuncoes(
                    eventoId,
                    usuarioId
                )

                conteudo.removeAllViews()

                adicionarTexto("Atividades da avaliação", 24f, true)

                adicionarTexto(
                    "Processo: ${evento.processo}\n" +
                            "Vara: ${evento.vara}\n" +
                            "Reclamante: ${evento.reclamante}\n" +
                            "Reclamada: ${evento.reclamada}"
                )

                val funcao = funcoes.firstOrNull {
                    it.id == funcaoSelecionadaId
                }

                if (funcao == null) {
                    funcaoSelecionadaId = null
                    mostrarFuncoes(funcoes)
                } else {
                    mostrarAtividades(
                        funcao,
                        banco.atividadeDao().listarPorFuncao(
                            funcao.id,
                            usuarioId
                        )
                    )
                }

                adicionarBotao("Voltar ao início") {
                    finish()
                }

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                mostrarMensagem("Não foi possível carregar a avaliação.")
            } finally {
                carregando = false
            }
        }
    }

    private fun mostrarFuncoes(funcoes: List<FuncaoEvento>) {
        adicionarTexto("Selecione a função", 20f, true)

        if (funcoes.isEmpty()) {
            adicionarTexto("Esta avaliação não possui funções cadastradas.")
        }

        funcoes.forEach { funcao ->
            adicionarBotao("${funcao.ordem}. ${funcao.nome}") {
                if (!carregando) {
                    funcaoSelecionadaId = funcao.id
                    carregarAvaliacao()
                }
            }
        }
    }

    private fun mostrarAtividades(
        funcao: FuncaoEvento,
        atividades: List<Atividade>
    ) {
        adicionarTexto("Função: ${funcao.nome}", 20f, true)

        adicionarBotao("Adicionar atividade") {
            editarNomeAtividade(funcao.id, null)
        }

        if (atividades.isEmpty()) {
            adicionarTexto("Nenhuma atividade cadastrada nesta função.")
        }

        atividades.forEachIndexed { indice, atividade ->
            adicionarTexto(
                "${indice + 1}. " +
                        atividade.nome.ifBlank { "Atividade sem nome" },
                20f,
                true
            )

            adicionarBotao("OCRA — Pausas e frequência") {
                abrirEtapa(
                    ChecklistOcraActivity::class.java,
                    atividade.id
                )
            }

            adicionarBotao("OCRA — Força") {
                abrirEtapa(
                    ForcaOcraActivity::class.java,
                    atividade.id
                )
            }

            adicionarBotao("OCRA — Posturas e estereotipia") {
                abrirEtapa(
                    PosturaOcraActivity::class.java,
                    atividade.id
                )
            }

            adicionarBotao("OCRA — Fatores complementares") {
                abrirEtapa(
                    ComplementaresOcraActivity::class.java,
                    atividade.id
                )
            }

            adicionarBotao("OCRA — Resultado da atividade") {
                abrirEtapa(
                    ResultadoOcraActivity::class.java,
                    atividade.id
                )
            }

            adicionarBotao("Alterar nome da atividade") {
                editarNomeAtividade(funcao.id, atividade)
            }
        }

        adicionarBotao("Escolher outra função") {
            if (!carregando) {
                funcaoSelecionadaId = null
                carregarAvaliacao()
            }
        }
    }

    private fun abrirEtapa(
        classe: Class<out AppCompatActivity>,
        atividadeId: Long
    ) {
        startActivity(
            Intent(this, classe).apply {
                putExtra("atividade_id", atividadeId)
            }
        )
    }

    private fun editarNomeAtividade(
        funcaoId: Long,
        atividade: Atividade?
    ) {
        val campo = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
            setText(atividade?.nome.orEmpty())
            setSelection(text?.length ?: 0)
        }

        val layoutCampo = TextInputLayout(this).apply {
            hint = "Nome da atividade"
            addView(campo)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(layoutCampo)
        }

        val dialogo = MaterialAlertDialogBuilder(this)
            .setTitle(
                if (atividade == null) "Nova atividade"
                else "Alterar nome da atividade"
            )
            .setView(container)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        var atividadeId = atividade?.id

        dialogo.setOnShowListener {
            val salvar = dialogo.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE
            )

            val cancelar = dialogo.getButton(
                android.content.DialogInterface.BUTTON_NEGATIVE
            )

            salvar.setOnClickListener {
                val nome = campo.text.toString().trim()

                if (nome.isEmpty()) {
                    layoutCampo.error = "Informe o nome da atividade."
                    return@setOnClickListener
                }

                layoutCampo.error = null
                salvar.isEnabled = false
                cancelar.isEnabled = false
                campo.isEnabled = false
                dialogo.setCancelable(false)

                lifecycleScope.launch {
                    try {
                        val id = atividadeId ?: banco.atividadeDao()
                            .criar(funcaoId, usuarioId)
                            .also { atividadeId = it }

                        banco.atividadeDao().salvarDados(
                            atividadeId = id,
                            usuarioId = usuarioId,
                            nome = nome,
                            descricao = atividade?.descricao.orEmpty(),
                            duracaoDiariaMinutos =
                                atividade?.duracaoDiariaMinutos
                        )

                        dialogo.dismiss()
                        carregarAvaliacao()

                    } catch (erro: CancellationException) {
                        throw erro
                    } catch (_: Exception) {
                        mostrarMensagem(
                            "Não foi possível salvar. Tente novamente."
                        )
                    } finally {
                        if (dialogo.isShowing) {
                            salvar.isEnabled = true
                            cancelar.isEnabled = true
                            campo.isEnabled = true
                            dialogo.setCancelable(true)
                        }
                    }
                }
            }
        }

        dialogo.show()
    }

    private fun adicionarTexto(
        texto: String,
        tamanho: Float = 15f,
        negrito: Boolean = false
    ) {
        conteudo.addView(
            TextView(this).apply {
                text = texto
                textSize = tamanho

                if (negrito) {
                    setTypeface(
                        typeface,
                        android.graphics.Typeface.BOLD
                    )
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                    bottomMargin = dp(12)
                }
            }
        )
    }

    private fun adicionarBotao(
        texto: String,
        acao: () -> Unit
    ) {
        conteudo.addView(
            MaterialButton(this).apply {
                text = texto
                isAllCaps = false
                minHeight = dp(56)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }

                setOnClickListener {
                    acao()
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        funcaoSelecionadaId?.let {
            outState.putLong(CHAVE_FUNCAO, it)
        }
        super.onSaveInstanceState(outState)
    }

    private fun mostrarMensagem(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }

    private fun voltarAoLogin() {
        sessao.encerrar()

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_EVENTO_ID = "evento_id"
        private const val CHAVE_FUNCAO = "funcao_selecionada"
    }
}