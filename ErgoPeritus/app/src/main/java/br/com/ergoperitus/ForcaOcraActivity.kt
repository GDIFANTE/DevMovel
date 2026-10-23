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
import br.com.ergoperitus.dados.ForcaOcra
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ForcaOcraActivity : AppCompatActivity() {

    private data class OpcaoTempo(
        val codigo: String,
        val descricao: String,
        val pontos: Int
    )

    private data class Grupo(
        val intensidade: MaterialButton,
        val tempo: MaterialButton
    )

    private data class Painel(
        val presenca: MaterialButton,
        val grupos: List<Grupo>,
        val observacoes: TextInputEditText,
        val resultado: TextView
    )

    private lateinit var modelo: ForcaOcraViewModel
    private lateinit var conteudo: LinearLayout
    private lateinit var rolagem: ScrollView
    private lateinit var status: TextView
    private lateinit var repetirSalvamento: MaterialButton
    private lateinit var voltar: MaterialButton

    private val paineis = mutableMapOf<String, Painel>()

    private var carregado = false
    private var saindo = false

    private val temposModerados = listOf(
        OpcaoTempo("TERCO", "Cerca de 1/3 do tempo", 2),
        OpcaoTempo("METADE", "Cerca da metade do tempo", 4),
        OpcaoTempo("MAIS_METADE", "Mais da metade do tempo", 6),
        OpcaoTempo("QUASE_TODO", "Quase o tempo todo", 8)
    )

    private val temposFortes = listOf(
        OpcaoTempo(
            "DOIS_SEGUNDOS",
            "2 segundos a cada 10 minutos",
            4
        ),
        OpcaoTempo("UM_PORCENTO", "1% do tempo", 8),
        OpcaoTempo("CINCO_PORCENTO", "5% do tempo", 16),
        OpcaoTempo("MAIS_DEZ_PORCENTO", "Mais de 10% do tempo", 24)
    )

    private val temposMaximos = listOf(
        OpcaoTempo(
            "DOIS_SEGUNDOS",
            "2 segundos a cada 10 minutos",
            6
        ),
        OpcaoTempo("UM_PORCENTO", "1% do tempo", 12),
        OpcaoTempo("CINCO_PORCENTO", "5% do tempo", 24),
        OpcaoTempo("MAIS_DEZ_PORCENTO", "Mais de 10% do tempo", 32)
    )

    private val valoresBorg = listOf(
        listOf(3, 4),
        listOf(5, 6, 7),
        listOf(8, 9, 10)
    )

    private val nomesGrupos = listOf(
        "Força moderada",
        "Força forte ou muito forte",
        "Força quase máxima"
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
            .get(ForcaOcraViewModel::class.java)

        montarTela()

        modelo.status.observe(this) {
            status.text = it
        }

        modelo.registros.observe(this) {
            atualizarPaineis()
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
                atualizarPaineis()

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                Toast.makeText(
                    this@ForcaOcraActivity,
                    "Não foi possível carregar a avaliação de força.",
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

        titulo("OCRA — Força")

        status = texto("Carregando...")

        texto(
            "Avalie separadamente cada braço. Nos grupos abaixo, " +
                    "registre somente as intensidades presentes e " +
                    "o tempo em que cada uma é aplicada."
        )

        texto(
            "Não conte o mesmo esforço em dois grupos. As intensidades " +
                    "de Borg 0 a 2 não recebem pontuação nos grupos " +
                    "de força desta ficha."
        )

        montarPainel("D", "Braço direito")
        montarPainel("E", "Braço esquerdo")

        repetirSalvamento = botao("Tentar salvar novamente") {
            lifecycleScope.launch {
                modelo.salvarAgora()
            }
        }

        voltar = botao("Voltar às atividades") {
            salvarESair()
        }

        atualizarPaineis()
    }

    private fun montarPainel(lado: String, nome: String) {
        titulo(nome)

        val presenca = botao("Força repetida: ainda não avaliada") {
            selecionarPresenca(lado)
        }

        val grupos = nomesGrupos.mapIndexed { indice, nomeGrupo ->
            texto(nomeGrupo).apply {
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val intensidade = botao("Selecionar intensidade de Borg") {
                selecionarBorg(lado, indice)
            }

            val tempo = botao("Selecionar tempo de aplicação") {
                selecionarTempo(lado, indice)
            }

            Grupo(intensidade, tempo)
        }

        val layout = TextInputLayout(this).apply {
            hint = "Ações que exigem força e observações"

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        val observacoes = TextInputEditText(layout.context).apply {
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

        val resultado = texto("Pontuação: aguardando avaliação.")

        paineis[lado] = Painel(
            presenca,
            grupos,
            observacoes,
            resultado
        )

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
                    if (carregado && !saindo) {
                        modelo.alterar(lado) {
                            it.copy(observacoes = s?.toString().orEmpty())
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )
    }

    private fun selecionarPresenca(lado: String) {
        val atual = modelo.registros.value?.get(lado) ?: return

        val indice = when (atual.haForcaRepetida) {
            null -> 0
            false -> 1
            true -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Força repetida pontuável no checklist")
            .setSingleChoiceItems(
                arrayOf(
                    "Ainda não avaliada",
                    "Não — sem força repetida de Borg 3 ou mais",
                    "Sim — há força repetida de Borg 3 ou mais"
                ),
                indice
            ) { dialogo, posicao ->
                dialogo.dismiss()

                val resposta = when (posicao) {
                    1 -> false
                    2 -> true
                    else -> null
                }

                val temIntensidades = listOf(
                    atual.borgModerada,
                    atual.borgForte,
                    atual.borgQuaseMaxima
                ).any { it != null }

                if (resposta != true && temIntensidades) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Limpar intensidades?")
                        .setMessage(
                            "Esta mudança apagará as intensidades e " +
                                    "os tempos registrados neste braço. " +
                                    "As observações serão mantidas."
                        )
                        .setPositiveButton("Confirmar") { _, _ ->
                            modelo.alterar(lado) {
                                it.copy(haForcaRepetida = resposta)
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    modelo.alterar(lado) {
                        it.copy(haForcaRepetida = resposta)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun descricaoBorg(valor: Int): String {
        return when (valor) {
            3 -> "3 — Moderada"
            4 -> "4 — Pouco intensa"
            5 -> "5 — Intensa"
            6 -> "6 — Entre intensa e muito intensa"
            7 -> "7 — Muito intensa"
            8 -> "8 — Entre muito intensa e muito, muito intensa"
            9 -> "9 — Muito, muito intensa"
            10 -> "10 — Máxima"
            else -> valor.toString()
        }
    }

    private fun borg(forca: ForcaOcra, grupo: Int): Int? {
        return when (grupo) {
            0 -> forca.borgModerada
            1 -> forca.borgForte
            else -> forca.borgQuaseMaxima
        }
    }

    private fun codigoTempo(forca: ForcaOcra, grupo: Int): String? {
        return when (grupo) {
            0 -> forca.tempoModerada
            1 -> forca.tempoForte
            else -> forca.tempoQuaseMaxima
        }
    }

    private fun tempos(grupo: Int): List<OpcaoTempo> {
        return when (grupo) {
            0 -> temposModerados
            1 -> temposFortes
            else -> temposMaximos
        }
    }

    private fun selecionarBorg(lado: String, grupo: Int) {
        val atual = modelo.registros.value?.get(lado) ?: return
        val valores = valoresBorg[grupo]

        val alternativas = arrayOf(
            "Não registrar este grupo",
            *valores.map { descricaoBorg(it) }.toTypedArray()
        )

        val selecionado = valores.indexOf(borg(atual, grupo)) + 1

        MaterialAlertDialogBuilder(this)
            .setTitle(nomesGrupos[grupo])
            .setSingleChoiceItems(
                alternativas,
                selecionado
            ) { dialogo, posicao ->
                val valor = if (posicao == 0) {
                    null
                } else {
                    valores[posicao - 1]
                }

                modelo.alterar(lado) {
                    when (grupo) {
                        0 -> it.copy(
                            borgModerada = valor,
                            tempoModerada = if (valor == null) {
                                null
                            } else {
                                it.tempoModerada
                            }
                        )

                        1 -> it.copy(
                            borgForte = valor,
                            tempoForte = if (valor == null) {
                                null
                            } else {
                                it.tempoForte
                            }
                        )

                        else -> it.copy(
                            borgQuaseMaxima = valor,
                            tempoQuaseMaxima = if (valor == null) {
                                null
                            } else {
                                it.tempoQuaseMaxima
                            }
                        )
                    }
                }

                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selecionarTempo(lado: String, grupo: Int) {
        val atual = modelo.registros.value?.get(lado) ?: return
        val opcoes = tempos(grupo)

        val alternativas = arrayOf(
            "Ainda não informado",
            *opcoes.map {
                "${it.descricao} — ${it.pontos} pontos"
            }.toTypedArray()
        )

        val indice = opcoes.indexOfFirst {
            it.codigo == codigoTempo(atual, grupo)
        } + 1

        MaterialAlertDialogBuilder(this)
            .setTitle("Tempo de aplicação da força")
            .setSingleChoiceItems(
                alternativas,
                indice
            ) { dialogo, posicao ->
                val codigo = if (posicao == 0) {
                    null
                } else {
                    opcoes[posicao - 1].codigo
                }

                modelo.alterar(lado) {
                    when (grupo) {
                        0 -> it.copy(tempoModerada = codigo)
                        1 -> it.copy(tempoForte = codigo)
                        else -> it.copy(tempoQuaseMaxima = codigo)
                    }
                }

                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarPaineis() {
        val mapa = modelo.registros.value
        val habilitado = carregado && !saindo

        paineis.forEach { (lado, painel) ->
            val forca = mapa?.get(lado)

            painel.presenca.isEnabled = habilitado

            painel.presenca.text = when (forca?.haForcaRepetida) {
                null -> "Força repetida: ainda não avaliada"
                false -> "Força repetida pontuável: não"
                true -> "Força repetida pontuável: sim"
            }

            painel.observacoes.isEnabled = habilitado

            val observacoes = forca?.observacoes.orEmpty()

            if (
                painel.observacoes.text.toString() != observacoes
            ) {
                painel.observacoes.setText(observacoes)
            }

            painel.grupos.forEachIndexed { indice, grupo ->
                val intensidade = forca?.let { borg(it, indice) }
                val tempo = forca?.let { codigoTempo(it, indice) }

                grupo.intensidade.isEnabled =
                    habilitado && forca?.haForcaRepetida == true

                grupo.tempo.isEnabled =
                    habilitado &&
                            forca?.haForcaRepetida == true &&
                            intensidade != null

                grupo.intensidade.text = intensidade?.let {
                    descricaoBorg(it)
                } ?: "Não registrado — selecionar se houver"

                grupo.tempo.text = tempos(indice).firstOrNull {
                    it.codigo == tempo
                }?.let {
                    "${it.descricao} — ${it.pontos} pontos"
                } ?: "Tempo de aplicação: selecionar"
            }

            painel.resultado.text = forca?.let {
                resultado(it)
            } ?: "Pontuação: aguardando avaliação."
        }

        if (::repetirSalvamento.isInitialized) {
            repetirSalvamento.isEnabled = habilitado
            voltar.isEnabled = habilitado
        }
    }

    private fun resultado(forca: ForcaOcra): String {
        if (forca.haForcaRepetida == null) {
            return "Pontuação: aguardando avaliação."
        }

        if (forca.haForcaRepetida == false) {
            return "Pontuação de força: 0 pontos."
        }

        val presentes = (0..2).filter {
            borg(forca, it) != null
        }

        if (presentes.isEmpty()) {
            return "Preenchimento incompleto: registre a intensidade " +
                    "e o tempo da força observada."
        }

        if (
            presentes.any { grupo ->
                codigoTempo(forca, grupo) == null
            }
        ) {
            return "Preenchimento incompleto: informe o tempo " +
                    "de cada intensidade registrada."
        }

        val total = presentes.sumOf { grupo ->
            tempos(grupo).first {
                it.codigo == codigoTempo(forca, grupo)
            }.pontos
        }

        val condicaoCritica =
            forca.tempoForte == "MAIS_DEZ_PORCENTO" ||
                    forca.tempoQuaseMaxima == "MAIS_DEZ_PORCENTO"

        return "Pontuação de força: $total pontos." +
                if (condicaoCritica) {
                    "\nAtenção: o checklist identifica esta condição " +
                            "de força e duração como não aceitável."
                } else {
                    ""
                }
    }

    private fun salvarESair() {
        if (saindo) return

        if (!carregado) {
            finish()
            return
        }

        saindo = true
        atualizarPaineis()

        lifecycleScope.launch {
            try {
                if (modelo.salvarAgora()) {
                    finish()
                }
            } finally {
                saindo = false

                if (!isFinishing) {
                    atualizarPaineis()
                }
            }
        }
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