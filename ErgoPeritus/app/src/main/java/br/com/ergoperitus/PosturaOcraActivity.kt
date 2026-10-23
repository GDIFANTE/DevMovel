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
import br.com.ergoperitus.dados.PosturaOcra
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PosturaOcraActivity : AppCompatActivity() {

    private data class Opcao(
        val valor: Double,
        val descricao: String
    )

    private lateinit var modelo: PosturaOcraViewModel
    private lateinit var conteudo: LinearLayout
    private lateinit var rolagem: ScrollView
    private lateinit var status: TextView
    private lateinit var seletorLado: MaterialButton
    private lateinit var preensoes: MaterialButton
    private lateinit var copiar: MaterialButton
    private lateinit var salvar: MaterialButton
    private lateinit var voltar: MaterialButton
    private lateinit var observacoes: TextInputEditText
    private lateinit var resultado: TextView

    private val botoes = mutableMapOf<String, MaterialButton>()

    private var carregado = false
    private var saindo = false
    private var preenchendoTela = false

    private val nomesPreensoes = arrayOf(
        "Pinça — dedos apertados",
        "Palmar — mão quase completamente aberta",
        "Gancho",
        "Outra preensão comparável"
    )

    private val bitsPreensoes = intArrayOf(1, 2, 4, 8)

    private val titulos = linkedMapOf(
        "ombro" to "Ombro",
        "acima" to "Mãos acima da cabeça",
        "cotovelo" to "Cotovelo",
        "punho" to "Punho",
        "mao" to "Mão e dedos — duração das preensões",
        "estereotipia" to "Estereotipia"
    )

    private val opcoes = mapOf(
        "ombro" to listOf(
            Opcao(0.0, "Não apresenta as condições descritas abaixo."),
            Opcao(
                1.0,
                "Braço sem apoio sobre o plano de trabalho, " +
                        "levantado durante pouco mais da metade do tempo."
            ),
            Opcao(
                2.0,
                "Braço sem apoio quase à altura do ombro, ou em outra " +
                        "postura extrema, durante cerca de 10% do tempo."
            ),
            Opcao(
                6.0,
                "Braço sem apoio quase à altura do ombro, ou em outra " +
                        "postura extrema, durante cerca de 1/3 do tempo."
            ),
            Opcao(
                12.0,
                "Braço sem apoio quase à altura do ombro, ou em outra " +
                        "postura extrema, durante mais da metade do tempo."
            ),
            Opcao(
                24.0,
                "Braço sem apoio quase à altura do ombro, ou em outra " +
                        "postura extrema, durante quase o tempo todo."
            )
        ),
        "acima" to listOf(
            Opcao(0.0, "Não."),
            Opcao(
                1.0,
                "Sim — dobrar a pontuação selecionada para o ombro."
            )
        ),
        "cotovelo" to listOf(
            Opcao(0.0, "Não apresenta as condições descritas abaixo."),
            Opcao(
                2.0,
                "Amplos movimentos de flexão/extensão ou " +
                        "pronação/supinação, movimentos bruscos, " +
                        "durante cerca de 1/3 do tempo."
            ),
            Opcao(
                4.0,
                "Amplos movimentos de flexão/extensão ou " +
                        "pronação/supinação, movimentos bruscos, " +
                        "durante mais da metade do tempo."
            ),
            Opcao(
                8.0,
                "Amplos movimentos de flexão/extensão ou " +
                        "pronação/supinação, movimentos bruscos, " +
                        "durante o tempo inteiro."
            )
        ),
        "punho" to listOf(
            Opcao(0.0, "Não apresenta as condições descritas abaixo."),
            Opcao(
                2.0,
                "Desvios extremos ou posições incômodas, com amplas " +
                        "flexões, extensões ou desvios laterais, " +
                        "durante pelo menos 1/3 do tempo."
            ),
            Opcao(
                4.0,
                "Desvios extremos ou posições incômodas " +
                        "durante mais da metade do tempo."
            ),
            Opcao(
                8.0,
                "Desvios extremos durante quase o tempo todo."
            )
        ),
        "mao" to listOf(
            Opcao(0.0, "Não apresenta as preensões pontuáveis descritas."),
            Opcao(2.0, "Preensões durante cerca de 1/3 do tempo."),
            Opcao(4.0, "Preensões durante mais da metade do tempo."),
            Opcao(8.0, "Preensões durante quase o tempo inteiro.")
        ),
        "estereotipia" to listOf(
            Opcao(0.0, "Não apresenta as condições descritas abaixo."),
            Opcao(
                1.5,
                "Gestos idênticos repetidos durante mais da metade " +
                        "do tempo; ou ciclos de 8 a 15 segundos com " +
                        "predomínio de ações técnicas dos membros superiores."
            ),
            Opcao(
                3.0,
                "Gestos idênticos repetidos durante quase o tempo todo; " +
                        "ou ciclos inferiores a 8 segundos com predomínio " +
                        "de ações técnicas dos membros superiores."
            )
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
            .get(PosturaOcraViewModel::class.java)

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
                    this@PosturaOcraActivity,
                    "Não foi possível carregar as posturas.",
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

        titulo("OCRA — Posturas")
        status = texto("Carregando...")

        texto(
            "Avalie cada lado separadamente. Para condições iguais, " +
                    "preencha um braço e use Aplicar também ao outro braço."
        )

        seletorLado = botao("Braço direito") {
            selecionarLado()
        }

        titulos.forEach { (chave, nome) ->
            titulo(nome)

            if (chave == "mao") {
                texto(
                    "Informe os tipos de preensão e sua duração. " +
                            "É possível selecionar mais de um tipo."
                )

                preensoes = botao("Tipos de preensão: selecionar") {
                    selecionarPreensoes()
                }
            }

            botoes[chave] = botao("Selecionar condição") {
                selecionarCondicao(chave)
            }
        }

        val layout = TextInputLayout(this).apply {
            hint = "Observações de postura"

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
            val origem = nomeLado(modelo.ladoSelecionado)
            val destino = if (modelo.ladoSelecionado == "D") {
                "braço esquerdo"
            } else {
                "braço direito"
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Copiar posturas")
                .setMessage(
                    "Copiar todas as respostas e observações do " +
                            "$origem para o $destino? As respostas " +
                            "anteriores do destino serão substituídas. " +
                            "Alterações futuras continuarão independentes."
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

    private fun valor(
        postura: PosturaOcra,
        chave: String
    ): Double? {
        return when (chave) {
            "ombro" -> postura.ombro?.toDouble()
            "acima" -> postura.maosAcimaCabeca?.let {
                if (it) 1.0 else 0.0
            }
            "cotovelo" -> postura.cotovelo?.toDouble()
            "punho" -> postura.punho?.toDouble()
            "mao" -> postura.maoDedos?.toDouble()
            else -> postura.estereotipia
        }
    }

    private fun selecionarCondicao(chave: String) {
        val postura = modelo.registros.value
            ?.get(modelo.ladoSelecionado) ?: return

        val alternativas = opcoes.getValue(chave)

        val textos = arrayOf(
            "Ainda não avaliado",
            *alternativas.map {
                if (chave == "acima") {
                    it.descricao
                } else {
                    "${formatar(it.valor)} pontos — ${it.descricao}"
                }
            }.toTypedArray()
        )

        val indice = alternativas.indexOfFirst {
            it.valor == valor(postura, chave)
        } + 1

        MaterialAlertDialogBuilder(this)
            .setTitle(titulos.getValue(chave))
            .setSingleChoiceItems(
                textos,
                indice
            ) { dialogo, posicao ->
                val escolhido = if (posicao == 0) {
                    null
                } else {
                    alternativas[posicao - 1].valor
                }

                modelo.alterar(modelo.ladoSelecionado) {
                    when (chave) {
                        "ombro" -> it.copy(
                            ombro = escolhido?.toInt()
                        )
                        "acima" -> it.copy(
                            maosAcimaCabeca = escolhido?.let { v ->
                                v == 1.0
                            }
                        )
                        "cotovelo" -> it.copy(
                            cotovelo = escolhido?.toInt()
                        )
                        "punho" -> it.copy(
                            punho = escolhido?.toInt()
                        )
                        "mao" -> it.copy(
                            maoDedos = escolhido?.toInt()
                        )
                        else -> it.copy(
                            estereotipia = escolhido
                        )
                    }
                }

                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarPreensoes() {
        val postura = modelo.registros.value
            ?.get(modelo.ladoSelecionado) ?: return

        val mascara = postura.preensoes ?: 0

        val marcadas = BooleanArray(4) {
            mascara and bitsPreensoes[it] != 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Tipos de preensão — desmarque tudo para Nenhum")
            .setMultiChoiceItems(
                nomesPreensoes,
                marcadas
            ) { _, indice, marcada ->
                marcadas[indice] = marcada
            }
            .setPositiveButton("Aplicar") { _, _ ->
                var combinado = 0

                marcadas.forEachIndexed { indice, marcada ->
                    if (marcada) {
                        combinado = combinado or bitsPreensoes[indice]
                    }
                }

                modelo.alterar(modelo.ladoSelecionado) {
                    it.copy(preensoes = combinado)
                }
            }
            .setNeutralButton("Ainda não avaliado") { _, _ ->
                modelo.alterar(modelo.ladoSelecionado) {
                    it.copy(preensoes = null)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarTela() {
        if (!::resultado.isInitialized) return

        val postura = modelo.registros.value
            ?.get(modelo.ladoSelecionado)

        val habilitado = carregado && !saindo

        seletorLado.text = if (modelo.ladoSelecionado == "D") {
            "Lado avaliado: braço direito"
        } else {
            "Lado avaliado: braço esquerdo"
        }

        seletorLado.isEnabled = habilitado
        preensoes.isEnabled = habilitado
        observacoes.isEnabled = habilitado
        copiar.isEnabled = habilitado
        salvar.isEnabled = habilitado
        voltar.isEnabled = habilitado

        botoes.forEach { (chave, botao) ->
            botao.isEnabled = habilitado

            val selecionada = postura?.let {
                valor(it, chave)
            }

            val opcao = opcoes.getValue(chave).firstOrNull {
                it.valor == selecionada
            }

            botao.text = when {
                opcao == null -> "Ainda não avaliado — selecionar"
                chave == "acima" -> opcao.descricao
                else -> "${formatar(opcao.valor)} pontos\n${opcao.descricao}"
            }
        }

        val mascara = postura?.preensoes

        preensoes.text = when (mascara) {
            null -> "Tipos de preensão: ainda não avaliados"
            0 -> "Tipos de preensão: nenhum dos descritos"
            else -> {
                val nomes = nomesPreensoes.filterIndexed { indice, _ ->
                    mascara and bitsPreensoes[indice] != 0
                }

                "Tipos de preensão:\n${nomes.joinToString("\n")}"
            }
        }

        val observacao = postura?.observacoes.orEmpty()

        if (observacoes.text.toString() != observacao) {
            preenchendoTela = true
            observacoes.setText(observacao)
            preenchendoTela = false
        }

        resultado.text = postura?.let {
            calcularResultado(it)
        } ?: "Pontuação: aguardando avaliação."
    }

    private fun calcularResultado(postura: PosturaOcra): String {
        val ombro = postura.ombro
        val cotovelo = postura.cotovelo
        val punho = postura.punho
        val mao = postura.maoDedos
        val estereotipia = postura.estereotipia
        val preensao = postura.preensoes

        if (
            ombro == null ||
            cotovelo == null ||
            punho == null ||
            mao == null ||
            estereotipia == null ||
            preensao == null ||
            postura.maosAcimaCabeca == null
        ) {
            return "Preenchimento incompleto: responda às posturas, " +
                    "aos tipos de preensão, à condição acima da cabeça " +
                    "e à estereotipia."
        }

        if (postura.maosAcimaCabeca && ombro == 0) {
            return "Revise o ombro: foi indicado trabalho acima da " +
                    "cabeça, mas nenhuma condição pontuável no ombro."
        }

        if (mao > 0 && preensao == 0) {
            return "Revise mão e dedos: selecione o tipo de preensão " +
                    "correspondente à duração informada."
        }

        if (mao == 0 && preensao > 0) {
            return "Revise mão e dedos: há preensões selecionadas. " +
                    "Confira se sua duração corresponde a uma condição " +
                    "pontuável e registre a justificativa nas observações."
        }

        val ombroAjustado = if (postura.maosAcimaCabeca) {
            ombro * 2
        } else {
            ombro
        }

        val maior = maxOf(ombroAjustado, cotovelo, punho, mao)
        val total = maior + estereotipia

        return "Ombro considerado: $ombroAjustado pontos\n" +
                "Maior valor entre as quatro regiões: $maior pontos\n" +
                "Estereotipia: ${formatar(estereotipia)} pontos\n" +
                "Pontuação de postura: ${formatar(total)} pontos"
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

    private fun nomeLado(lado: String): String {
        return if (lado == "D") "braço direito" else "braço esquerdo"
    }

    private fun mostrarCampoEmFoco() {
        val campo = currentFocus as? TextInputEditText ?: return

        campo.requestRectangleOnScreen(
            Rect(0, 0, campo.width, campo.height + dp(24)),
            true
        )
    }

    private fun formatar(valor: Double): String {
        return if (valor % 1.0 == 0.0) {
            valor.toInt().toString()
        } else {
            valor.toString().replace('.', ',')
        }
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