package br.com.ergoperitus

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.ComplementaresOcra
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ComplementaresOcraActivity : AppCompatActivity() {

    private data class Fator(
        val bit: Int,
        val descricao: String
    )

    private data class Opcao(
        val pontos: Int,
        val descricao: String
    )

    private lateinit var modelo: ComplementaresOcraViewModel
    private lateinit var conteudo: LinearLayout
    private lateinit var rolagem: ScrollView
    private lateinit var status: TextView

    private lateinit var lado: MaterialButton
    private lateinit var fatores: MaterialButton
    private lateinit var fisicos: MaterialButton
    private lateinit var ritmo: MaterialButton
    private lateinit var copiar: MaterialButton
    private lateinit var salvar: MaterialButton
    private lateinit var voltar: MaterialButton

    private lateinit var observacoes: TextInputEditText
    private lateinit var resultado: TextView

    private var carregado = false
    private var saindo = false
    private var preenchendoTela = false

    private val listaFatores = listOf(
        Fator(
            ComplementaresOcra.LUVAS_INADEQUADAS,
            "Luvas inadequadas à preensão, incômodas, espessas ou " +
                    "de tamanho inadequado, por mais da metade do tempo."
        ),
        Fator(
            ComplementaresOcra.MOVIMENTOS_BRUSCOS,
            "Movimentos bruscos, de arranque ou contragolpes, " +
                    "com frequência de 2 por minuto ou mais."
        ),
        Fator(
            ComplementaresOcra.IMPACTOS,
            "Impactos repetidos com as mãos, com frequência " +
                    "de pelo menos 10 vezes por hora."
        ),
        Fator(
            ComplementaresOcra.FRIO,
            "Contato com superfícies abaixo de 0 °C ou trabalho " +
                    "em câmaras frigoríficas por mais da metade do tempo."
        ),
        Fator(
            ComplementaresOcra.VIBRACAO,
            "Ferramentas vibratórias ou parafusadeiras com " +
                    "contragolpe por pelo menos 1/3 do tempo."
        ),
        Fator(
            ComplementaresOcra.COMPRESSAO,
            "Ferramentas que comprimem estruturas " +
                    "musculotendíneas, com sinais como vermelhidão ou calos."
        ),
        Fator(
            ComplementaresOcra.PRECISAO,
            "Trabalho de precisão em áreas inferiores a 2–3 mm, " +
                    "com visão próxima, por mais da metade do tempo."
        ),
        Fator(
            ComplementaresOcra.OUTROS,
            "Outros fatores complementares — descrever " +
                    "nas observações, incluindo a duração."
        )
    )

    private val opcoesFisicas = listOf(
        Opcao(
            0,
            "Nenhuma condição física complementar pontuável."
        ),
        Opcao(
            2,
            "Condição física complementar correspondente às " +
                    "alternativas de 2 pontos do checklist; ou vários " +
                    "fatores que, no total, ocupam mais da metade do tempo."
        ),
        Opcao(
            3,
            "Um ou mais fatores complementares " +
                    "ocupam quase o tempo todo."
        ),
        Opcao(
            4,
            "Ferramentas com elevado conteúdo de vibrações, " +
                    "como martelo pneumático ou esmeril com cabo flexível, " +
                    "utilizadas durante pelo menos 1/3 do tempo."
        )
    )

    private val opcoesRitmo = listOf(
        Opcao(
            0,
            "Sem ritmo de trabalho imposto pela máquina."
        ),
        Opcao(
            1,
            "Ritmo determinado pela máquina, mas com áreas de " +
                    "pulmão que permitem acelerar ou desacelerar."
        ),
        Opcao(
            2,
            "Ritmo completamente determinado pela máquina."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usuarioId = SessaoUsuario(applicationContext).usuarioId()

        if (usuarioId == null) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        val atividadeId = intent.getLongExtra(EXTRA_ATIVIDADE_ID, 0L)

        if (atividadeId <= 0L) {
            finish()
            return
        }

        modelo = ViewModelProvider(this)
            .get(ComplementaresOcraViewModel::class.java)

        savedInstanceState?.getString("lado")?.let {
            if (it == "D" || it == "E") {
                modelo.ladoSelecionado = it
            }
        }

        montarTela()

        modelo.status.observe(this) {
            status.text = it
        }

        modelo.registros.observe(this) {
            atualizarTela()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    salvarESair()
                }
            }
        )

        lifecycleScope.launch {
            try {
                modelo.carregar(atividadeId, usuarioId)
                carregado = true
                atualizarTela()

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                Toast.makeText(
                    this@ComplementaresOcraActivity,
                    "Não foi possível carregar os fatores complementares.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun montarTela() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        rolagem = ScrollView(this).apply {
            isFillViewport = true
            isSaveEnabled = false
            clipToPadding = false
            addView(conteudo)
        }

        setContentView(rolagem)

        ViewCompat.setOnApplyWindowInsetsListener(rolagem) { view, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            val teclado = insets.getInsets(
                WindowInsetsCompat.Type.ime()
            )

            view.setPadding(
                barras.left,
                barras.top,
                barras.right,
                maxOf(barras.bottom, teclado.bottom)
            )

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                view.post {
                    mostrarCampoEmFoco()
                }
            }

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.requestApplyInsets(rolagem)

        titulo("OCRA — Fatores complementares")
        status = texto("Carregando...")

        texto(
            "Marque todos os fatores observados. Depois escolha " +
                    "uma pontuação para o bloco físico e uma para " +
                    "o ritmo imposto pela máquina."
        )

        texto(
            "Os pontos dos fatores físicos não são somados " +
                    "individualmente. O total desta etapa é a pontuação " +
                    "física mais a pontuação do ritmo."
        )

        lado = botao("Lado avaliado: braço direito") {
            selecionarLado()
        }

        titulo("Fatores observados")

        fatores = botao("Selecionar fatores") {
            selecionarFatores()
        }

        titulo("Pontuação física")

        fisicos = botao("Selecionar pontuação do bloco físico") {
            val atual = registroAtual() ?: return@botao

            selecionarOpcao(
                "Pontuação do bloco físico",
                opcoesFisicas,
                atual.pontuacaoFisica
            ) { pontos ->
                modelo.alterar(modelo.ladoSelecionado) {
                    it.copy(pontuacaoFisica = pontos)
                }
            }
        }

        titulo("Ritmo imposto pela máquina")

        ritmo = botao("Selecionar condição de ritmo") {
            val atual = registroAtual() ?: return@botao

            selecionarOpcao(
                "Ritmo imposto pela máquina",
                opcoesRitmo,
                atual.ritmoMaquina
            ) { pontos ->
                modelo.alterar(modelo.ladoSelecionado) {
                    it.copy(ritmoMaquina = pontos)
                }
            }
        }

        val layout = TextInputLayout(this).apply {
            hint = "Observações, outros fatores e duração"

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        }

        observacoes = TextInputEditText(layout.context).apply {
            isSaveEnabled = false
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
            gravity = Gravity.TOP

            setOnFocusChangeListener { _, foco ->
                if (foco) {
                    rolagem.post {
                        mostrarCampoEmFoco()
                    }
                }
            }
        }

        layout.addView(observacoes)
        conteudo.addView(layout)

        observacoes.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (carregado && !saindo && !preenchendoTela) {
                        modelo.alterar(modelo.ladoSelecionado) {
                            it.copy(
                                observacoes = s?.toString().orEmpty()
                            )
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )

        resultado = texto("Pontuação: aguardando avaliação.")

        copiar = botao("Aplicar também ao outro braço") {
            MaterialAlertDialogBuilder(this)
                .setTitle("Copiar fatores complementares")
                .setMessage(
                    "Copiar os fatores, as pontuações e as observações " +
                            "para o outro braço? As respostas anteriores " +
                            "do destino serão substituídas. Alterações " +
                            "futuras continuarão independentes."
                )
                .setPositiveButton("Copiar") { _, _ ->
                    modelo.copiarParaOutroBraco(
                        modelo.ladoSelecionado
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        salvar = botao("Tentar salvar novamente") {
            lifecycleScope.launch {
                modelo.salvarAgora()
            }
        }

        voltar = botao("Voltar às atividades") {
            salvarESair()
        }

        atualizarTela()
    }

    private fun registroAtual(): ComplementaresOcra? {
        return modelo.registros.value?.get(modelo.ladoSelecionado)
    }

    private fun selecionarLado() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Lado avaliado")
            .setSingleChoiceItems(
                arrayOf("Braço direito", "Braço esquerdo"),
                if (modelo.ladoSelecionado == "D") 0 else 1
            ) { dialogo, posicao ->
                observacoes.clearFocus()
                modelo.ladoSelecionado =
                    if (posicao == 0) "D" else "E"
                atualizarTela()
                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarFatores() {
        val atual = registroAtual() ?: return
        val mascara = atual.fatores ?: 0

        val marcados = BooleanArray(listaFatores.size) {
            mascara and listaFatores[it].bit != 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Fatores — desmarque todos para Nenhum")
            .setMultiChoiceItems(
                listaFatores.map {
                    it.descricao
                }.toTypedArray(),
                marcados
            ) { _, indice, marcado ->
                marcados[indice] = marcado
            }
            .setPositiveButton("Aplicar") { _, _ ->
                var combinado = 0

                marcados.forEachIndexed { indice, marcado ->
                    if (marcado) {
                        combinado = combinado or listaFatores[indice].bit
                    }
                }

                modelo.alterar(modelo.ladoSelecionado) {
                    // Uma mudança nos fatores exige revisar
                    // a pontuação física correspondente.
                    it.copy(
                        fatores = combinado,
                        pontuacaoFisica = if (it.fatores == combinado) {
                            it.pontuacaoFisica
                        } else {
                            null
                        }
                    )
                }
            }
            .setNeutralButton("Ainda não avaliado") { _, _ ->
                modelo.alterar(modelo.ladoSelecionado) {
                    it.copy(
                        fatores = null,
                        pontuacaoFisica = null
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarOpcao(
        titulo: String,
        opcoes: List<Opcao>,
        selecionada: Int?,
        aoSelecionar: (Int?) -> Unit
    ) {
        val alternativas = arrayOf(
            "Ainda não avaliado",
            *opcoes.map {
                "${it.pontos} pontos — ${it.descricao}"
            }.toTypedArray()
        )

        val indice = opcoes.indexOfFirst {
            it.pontos == selecionada
        } + 1

        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setSingleChoiceItems(
                alternativas,
                indice
            ) { dialogo, posicao ->
                aoSelecionar(
                    if (posicao == 0) {
                        null
                    } else {
                        opcoes[posicao - 1].pontos
                    }
                )
                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun textoOpcao(
        opcoes: List<Opcao>,
        pontos: Int?
    ): String {
        val opcao = opcoes.firstOrNull {
            it.pontos == pontos
        }

        return opcao?.let {
            "${it.pontos} pontos\n${it.descricao}"
        } ?: "Ainda não avaliado — selecionar"
    }

    private fun atualizarTela() {
        if (!::resultado.isInitialized) return

        val atual = registroAtual()
        val habilitado = carregado && !saindo

        lado.text = if (modelo.ladoSelecionado == "D") {
            "Lado avaliado: braço direito"
        } else {
            "Lado avaliado: braço esquerdo"
        }

        listOf(
            lado,
            fatores,
            fisicos,
            ritmo,
            copiar,
            salvar,
            voltar
        ).forEach {
            it.isEnabled = habilitado
        }

        observacoes.isEnabled = habilitado

        val mascara = atual?.fatores

        fatores.text = when (mascara) {
            null -> "Fatores: ainda não avaliados"
            0 -> "Fatores físicos: nenhum dos descritos"
            else -> {
                val selecionados = listaFatores.filter {
                    mascara and it.bit != 0
                }

                "Fatores observados:\n" +
                        selecionados.joinToString("\n\n") {
                            it.descricao
                        }
            }
        }

        fisicos.text = textoOpcao(
            opcoesFisicas,
            atual?.pontuacaoFisica
        )

        ritmo.text = textoOpcao(
            opcoesRitmo,
            atual?.ritmoMaquina
        )

        val observacao = atual?.observacoes.orEmpty()

        if (observacoes.text.toString() != observacao) {
            preenchendoTela = true
            observacoes.setText(observacao)
            preenchendoTela = false
        }

        resultado.text = atual?.let {
            calcularResultado(it)
        } ?: "Pontuação: aguardando avaliação."
    }

    private fun calcularResultado(
        registro: ComplementaresOcra
    ): String {
        val fatores = registro.fatores
        val fisica = registro.pontuacaoFisica
        val ritmo = registro.ritmoMaquina

        if (fatores == null || fisica == null || ritmo == null) {
            return "Preenchimento incompleto: avalie os fatores, " +
                    "a pontuação física e o ritmo da máquina."
        }

        if (fatores == 0 && fisica != 0) {
            return "Revise a pontuação física: nenhum fator " +
                    "foi selecionado."
        }

        if (fatores != 0 && fisica == 0) {
            return "Revise os fatores e a pontuação física: " +
                    "há condições pontuáveis selecionadas."
        }

        if (
            fisica == 4 &&
            fatores and ComplementaresOcra.VIBRACAO == 0
        ) {
            return "Revise a pontuação de 4 pontos: ela corresponde " +
                    "à condição de elevada vibração descrita na alternativa."
        }

        if (
            fatores and ComplementaresOcra.OUTROS != 0 &&
            registro.observacoes.isBlank()
        ) {
            return "Descreva os outros fatores e sua duração " +
                    "nas observações."
        }

        return "Bloco físico: $fisica pontos\n" +
                "Ritmo da máquina: $ritmo pontos\n" +
                "Pontuação dos complementares: ${fisica + ritmo} pontos"
    }

    private fun salvarESair() {
        if (saindo) return

        if (!carregado) {
            finish()
            return
        }

        saindo = true
        atualizarTela()

        lifecycleScope.launch {
            try {
                if (modelo.salvarAgora()) {
                    finish()
                }
            } finally {
                saindo = false

                if (!isFinishing) {
                    atualizarTela()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::modelo.isInitialized) {
            outState.putString("lado", modelo.ladoSelecionado)
        }
        super.onSaveInstanceState(outState)
    }

    private fun mostrarCampoEmFoco() {
        val campo = currentFocus as? TextInputEditText ?: return

        campo.requestRectangleOnScreen(
            Rect(0, 0, campo.width, campo.height + dp(24)),
            true
        )
    }

    private fun texto(valor: String): TextView {
        val view = TextView(this).apply {
            text = valor
            textSize = 15f

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        conteudo.addView(view)
        return view
    }

    private fun titulo(valor: String) {
        texto(valor).apply {
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun botao(
        valor: String,
        acao: () -> Unit
    ): MaterialButton {
        val view = MaterialButton(this).apply {
            text = valor
            isAllCaps = false
            minHeight = dp(56)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }

            setOnClickListener {
                acao()
            }
        }

        conteudo.addView(view)
        return view
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_ATIVIDADE_ID = "atividade_id"
    }
}