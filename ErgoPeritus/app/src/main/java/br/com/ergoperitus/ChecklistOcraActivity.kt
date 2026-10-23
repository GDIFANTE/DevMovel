package br.com.ergoperitus

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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
import br.com.ergoperitus.ChecklistOcraViewModel.Lado
import br.com.ergoperitus.dados.ChecklistOcra
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

class ChecklistOcraActivity : AppCompatActivity() {

    private data class Campo(
        val layout: TextInputLayout,
        val texto: TextInputEditText
    )

    private data class Opcao(
        val pontos: Double,
        val descricao: String
    )

    private data class PainelFrequencia(
        val resumo: TextView,
        val iniciar: MaterialButton,
        val adicionar: MaterialButton,
        val desfazer: MaterialButton,
        val interromper: MaterialButton,
        val dinamica: MaterialButton,
        val estatica: MaterialButton,
        val resultado: TextView
    )

    private lateinit var modelo: ChecklistOcraViewModel
    private lateinit var rolagem: ScrollView
    private lateinit var conteudo: LinearLayout

    private lateinit var nome: Campo
    private lateinit var descricao: Campo
    private lateinit var duracao: Campo
    private lateinit var jornada: Campo
    private lateinit var distribuicao: Campo

    private lateinit var status: TextView
    private lateinit var botaoPausas: MaterialButton
    private lateinit var botaoSalvar: MaterialButton
    private lateinit var botaoVoltar: MaterialButton

    private val paineis = mutableMapOf<Lado, PainelFrequencia>()

    private var recuperacaoSelecionada: Int? = null
    private var carregado = false
    private var saindo = false
    private var formularioValido = true

    private val opcoesPausas = listOf(
        Opcao(
            0.0,
            "Interrupção de pelo menos 8–10 minutos a cada hora, " +
                    "contando a refeição, ou recuperação dentro do ciclo."
        ),
        Opcao(
            2.0,
            "Duas interrupções de manhã e duas à tarde, além da refeição, " +
                    "de 8–10 minutos, em turno de 7–8 horas; ou quatro " +
                    "interrupções de 8–10 minutos em turno de 6 horas."
        ),
        Opcao(
            3.0,
            "Duas pausas de 8–10 minutos em turno de cerca de 6 horas " +
                    "sem refeição; ou três pausas além da refeição " +
                    "em turno de 7–8 horas."
        ),
        Opcao(
            4.0,
            "Duas interrupções de 8–10 minutos além da refeição em turno " +
                    "de 7–8 horas, ou três sem refeição; ou uma pausa " +
                    "de 8–10 minutos em turno de 6 horas."
        ),
        Opcao(
            6.0,
            "Uma pausa de pelo menos 10 minutos em turno de cerca de " +
                    "7 horas sem refeição; ou somente a pausa para " +
                    "refeição em turno de 8 horas, com refeição " +
                    "fora do horário de trabalho."
        ),
        Opcao(
            10.0,
            "Sem interrupções efetivas, exceto pausas de poucos minutos " +
                    "(menos de 5), em turno de 7–8 horas."
        )
    )

    private val opcoesDinamicas = listOf(
        Opcao(
            0.0,
            "Movimentos lentos, cerca de 20 ações/minuto, " +
                    "com possibilidade de frequentes interrupções."
        ),
        Opcao(
            1.0,
            "Movimentos não muito velozes, cerca de 30 ações/minuto, " +
                    "com possibilidade de breves interrupções."
        ),
        Opcao(
            3.0,
            "Movimentos mais rápidos, cerca de 40 ações/minuto, " +
                    "com possibilidade de breves interrupções."
        ),
        Opcao(
            4.0,
            "Movimentos bastante rápidos, cerca de 40 ações/minuto, " +
                    "com interrupções escassas e irregulares."
        ),
        Opcao(
            6.0,
            "Movimentos rápidos e constantes, cerca de 50 ações/minuto, " +
                    "com pausas apenas ocasionais e breves."
        ),
        Opcao(
            8.0,
            "Movimentos muito rápidos e constantes, cerca de " +
                    "60 ações/minuto; a falta de interrupções " +
                    "dificulta manter o ritmo."
        ),
        Opcao(
            10.0,
            "Frequência elevadíssima, 70 ou mais ações/minuto, " +
                    "sem possibilidade de interrupções."
        )
    )

    private val opcoesEstaticas = listOf(
        Opcao(
            0.0,
            "Não apresenta as condições de preensão estática " +
                    "descritas nas alternativas abaixo."
        ),
        Opcao(
            2.5,
            "Objeto mantido em preensão estática por pelo menos " +
                    "5 segundos, ocupando 2/3 do ciclo ou da observação."
        ),
        Opcao(
            4.5,
            "Objeto mantido em preensão estática por pelo menos " +
                    "5 segundos, ocupando todo o ciclo ou a observação."
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
            .get(ChecklistOcraViewModel::class.java)

        montarTela()

        modelo.status.observe(this) {
            atualizarStatus()
        }

        modelo.checklistVisivel.observe(this) {
            atualizarPaineis(it)
        }

        modelo.ladoEmMedicao.observe(this) { lado ->
            if (lado != null) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }

            modelo.checklistVisivel.value?.let {
                atualizarPaineis(it)
            }
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
                val formulario = modelo.carregar(
                    atividadeId,
                    usuarioId
                )

                nome.texto.setText(formulario.atividade.nome)
                descricao.texto.setText(formulario.atividade.descricao)

                duracao.texto.setText(
                    formulario.atividade.duracaoDiariaMinutos
                        ?.toString().orEmpty()
                )

                jornada.texto.setText(
                    formulario.checklist.jornadaMinutos
                        ?.toString().orEmpty()
                )

                distribuicao.texto.setText(
                    formulario.checklist.distribuicaoPausas
                )

                recuperacaoSelecionada =
                    formulario.checklist.recuperacao

                atualizarBotaoPausas()
                configurarObservadores()

                carregado = true
                habilitarFormulario(true)

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                Toast.makeText(
                    this@ChecklistOcraActivity,
                    "Não foi possível carregar a atividade.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onStop() {
        if (
            ::modelo.isInitialized &&
            !isChangingConfigurations
        ) {
            modelo.interromperMedicao()
        }

        super.onStop()
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

        adicionarTitulo("Checklist OCRA")

        status = adicionarTexto("Carregando...").apply {
            accessibilityLiveRegion =
                View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

        nome = adicionarCampo("Nome da atividade")

        descricao = adicionarCampo(
            "Descrição da atividade",
            multiline = true
        )

        duracao = adicionarCampo(
            "Duração diária desta atividade em minutos",
            numerico = true
        )

        adicionarTitulo("Pausas e recuperação")

        jornada = adicionarCampo(
            "Duração efetiva do turno em minutos",
            numerico = true
        )

        botaoPausas = adicionarBotao(
            "Selecionar condição de pausas"
        ) {
            selecionarOpcao(
                "Pausas e recuperação",
                opcoesPausas,
                recuperacaoSelecionada?.toDouble()
            ) {
                recuperacaoSelecionada = it?.toInt()
                atualizarBotaoPausas()
                atualizarFormulario()
            }
        }

        distribuicao = adicionarCampo(
            "Distribuição das pausas no turno",
            multiline = true
        )

        adicionarTexto(
            "Exemplo: início às 07h; pausa às 09h; " +
                    "refeição das 12h às 13h; pausa às 15h; " +
                    "término às 17h."
        )

        adicionarTitulo("Frequência de ações")

        adicionarTexto(
            "Cada toque no + corresponde a uma ação técnica do braço " +
                    "observado, e não necessariamente a uma peça ou ciclo. " +
                    "Conte cada lado separadamente durante 60 segundos."
        )

        adicionarTexto(
            "A pontuação dinâmica também depende das interrupções. " +
                    "Após contar, selecione a descrição correspondente. " +
                    "As frequências dinâmica e estática são avaliadas " +
                    "separadamente; prevalece a maior pontuação."
        )

        montarPainel(Lado.DIREITO)
        montarPainel(Lado.ESQUERDO)

        botaoSalvar = adicionarBotao(
            "Tentar salvar novamente"
        ) {
            if (atualizarFormulario()) {
                lifecycleScope.launch {
                    modelo.salvarAgora()
                }
            }
        }

        botaoVoltar = adicionarBotao("Voltar às atividades") {
            salvarESair()
        }

        habilitarFormulario(false)
    }

    private fun montarPainel(lado: Lado) {
        val titulo = if (lado == Lado.DIREITO) {
            "Braço direito"
        } else {
            "Braço esquerdo"
        }

        adicionarTitulo(titulo)

        val resumo = adicionarTexto("Nenhuma medição realizada.").apply {
            textSize = 20f
        }

        val iniciar = adicionarBotao("Iniciar 60 segundos") {
            if (!atualizarFormulario()) return@adicionarBotao

            val checklist = modelo.checklistVisivel.value
                ?: return@adicionarBotao

            val tempo = if (lado == Lado.DIREITO) {
                checklist.tempoDireitaMs
            } else {
                checklist.tempoEsquerdaMs
            }

            val contagem = if (lado == Lado.DIREITO) {
                checklist.contagemDireita
            } else {
                checklist.contagemEsquerda
            }

            if (tempo > 0L || contagem > 0) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Repetir medição")
                    .setMessage(
                        "A contagem anterior deste braço será substituída. " +
                                "A seleção dinâmica também será apagada " +
                                "para ser reavaliada."
                    )
                    .setPositiveButton("Iniciar do zero") { _, _ ->
                        modelo.iniciarMedicao(lado)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                modelo.iniciarMedicao(lado)
            }
        }

        val adicionar = adicionarBotao("+  1 ação") {
            modelo.contar(lado, 1)
        }.apply {
            textSize = 28f
            minHeight = dp(88)
            contentDescription = "Adicionar uma ação ao $titulo"
        }

        val desfazer = adicionarBotao("Desfazer último toque") {
            modelo.contar(lado, -1)
        }

        val interromper = adicionarBotao("Interromper medição") {
            modelo.interromperMedicao()
        }

        val dinamica = adicionarBotao("Selecionar condição dinâmica") {
            val atual = modelo.checklistVisivel.value
                ?: return@adicionarBotao

            val pontos = if (lado == Lado.DIREITO) {
                atual.frequenciaDinamicaDireita
            } else {
                atual.frequenciaDinamicaEsquerda
            }

            selecionarOpcao(
                "$titulo — ações dinâmicas",
                opcoesDinamicas,
                pontos
            ) {
                modelo.selecionarFrequencia(lado, false, it)
            }
        }

        val estatica = adicionarBotao("Selecionar condição estática") {
            val atual = modelo.checklistVisivel.value
                ?: return@adicionarBotao

            val pontos = if (lado == Lado.DIREITO) {
                atual.frequenciaEstaticaDireita
            } else {
                atual.frequenciaEstaticaEsquerda
            }

            selecionarOpcao(
                "$titulo — ações estáticas",
                opcoesEstaticas,
                pontos
            ) {
                modelo.selecionarFrequencia(lado, true, it)
            }
        }

        val resultado = adicionarTexto(
            "Pontuação de frequência: aguardando respostas."
        )

        paineis[lado] = PainelFrequencia(
            resumo,
            iniciar,
            adicionar,
            desfazer,
            interromper,
            dinamica,
            estatica,
            resultado
        )
    }

    private fun atualizarPaineis(checklist: ChecklistOcra) {
        val ativo = modelo.ladoEmMedicao.value

        paineis.forEach { (lado, painel) ->
            val direito = lado == Lado.DIREITO

            val contagem = if (direito) {
                checklist.contagemDireita
            } else {
                checklist.contagemEsquerda
            }

            val tempo = if (direito) {
                checklist.tempoDireitaMs
            } else {
                checklist.tempoEsquerdaMs
            }

            val concluida = if (direito) {
                checklist.medicaoDireitaConcluida
            } else {
                checklist.medicaoEsquerdaConcluida
            }

            val dinamica = if (direito) {
                checklist.frequenciaDinamicaDireita
            } else {
                checklist.frequenciaDinamicaEsquerda
            }

            val estatica = if (direito) {
                checklist.frequenciaEstaticaDireita
            } else {
                checklist.frequenciaEstaticaEsquerda
            }

            val medindoEste = ativo == lado
            val segundosRestantes = (
                    (60_000L - tempo + 999L) / 1000L
                    ).coerceAtLeast(0L)

            painel.resumo.text = when {
                medindoEste ->
                    "$contagem ações\n" +
                            "Restam $segundosRestantes segundos"

                concluida ->
                    "Medição concluída: 60 segundos\n" +
                            "$contagem ações/minuto"

                tempo > 0L || contagem > 0 ->
                    "Medição incompleta: $contagem ações\n" +
                            "Tempo observado: " +
                            String.format(
                                Locale.getDefault(),
                                "%.1f segundos",
                                tempo / 1000.0
                            ) +
                            "\nRepita a medição por 60 segundos."

                else ->
                    "Nenhuma medição realizada."
            }

            painel.iniciar.text = if (
                tempo > 0L || contagem > 0
            ) {
                "Repetir 60 segundos"
            } else {
                "Iniciar 60 segundos"
            }

            painel.dinamica.text = textoOpcao(
                "Condição dinâmica",
                opcoesDinamicas,
                dinamica
            )

            painel.estatica.text = textoOpcao(
                "Condição estática",
                opcoesEstaticas,
                estatica
            )

            painel.resultado.text = if (
                dinamica != null && estatica != null
            ) {
                "Pontuação de frequência: " +
                        formatarPontos(maxOf(dinamica, estatica)) +
                        " pontos"
            } else {
                "Pontuação de frequência: " +
                        "responda às condições dinâmica e estática."
            }

            val habilitado = carregado && !saindo

            painel.iniciar.isEnabled =
                habilitado && ativo == null

            painel.adicionar.isEnabled =
                habilitado && medindoEste

            painel.desfazer.isEnabled =
                habilitado && medindoEste && contagem > 0

            painel.interromper.isEnabled =
                habilitado && medindoEste

            painel.dinamica.isEnabled =
                habilitado && ativo == null

            painel.estatica.isEnabled =
                habilitado && ativo == null
        }
    }

    private fun selecionarOpcao(
        titulo: String,
        opcoes: List<Opcao>,
        selecionada: Double?,
        aoSelecionar: (Double?) -> Unit
    ) {
        val alternativas = arrayOf(
            "Ainda não informado",
            *opcoes.map {
                "${formatarPontos(it.pontos)} pontos — ${it.descricao}"
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
        titulo: String,
        opcoes: List<Opcao>,
        pontos: Double?
    ): String {
        val opcao = opcoes.firstOrNull {
            it.pontos == pontos
        }

        return if (opcao == null) {
            "$titulo: selecionar"
        } else {
            "$titulo: ${formatarPontos(opcao.pontos)} pontos\n" +
                    opcao.descricao
        }
    }

    private fun formatarPontos(valor: Double): String {
        return if (valor % 1.0 == 0.0) {
            valor.toInt().toString()
        } else {
            valor.toString().replace('.', ',')
        }
    }

    private fun mostrarCampoEmFoco() {
        val campo = currentFocus ?: return
        if (campo !is TextInputEditText) return

        campo.requestRectangleOnScreen(
            Rect(0, 0, campo.width, campo.height + dp(24)),
            true
        )
    }

    private fun atualizarStatus() {
        status.text = if (formularioValido) {
            modelo.status.value.orEmpty()
        } else {
            "Existe um valor inválido. Corrija para salvar o formulário."
        }
    }

    private fun configurarObservadores() {
        listOf(
            nome,
            descricao,
            duracao,
            jornada,
            distribuicao
        ).forEach { campo ->
            campo.texto.addTextChangedListener(
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
                            atualizarFormulario()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) = Unit
                }
            )
        }
    }

    private fun atualizarFormulario(): Boolean {
        if (!carregado) return false

        val duracaoTexto = duracao.texto.text.toString().trim()
        val jornadaTexto = jornada.texto.text.toString().trim()

        val minutosAtividade = duracaoTexto.toIntOrNull()
        val minutosJornada = jornadaTexto.toIntOrNull()

        val duracaoValida = duracaoTexto.isEmpty() ||
                (
                        minutosAtividade != null &&
                                minutosAtividade in 1..1440
                        )

        val jornadaValida = jornadaTexto.isEmpty() ||
                (
                        minutosJornada != null &&
                                minutosJornada in 1..1440
                        )

        duracao.layout.error = if (duracaoValida) {
            null
        } else {
            "Informe de 1 a 1440 minutos ou deixe vazio."
        }

        jornada.layout.error = if (jornadaValida) {
            null
        } else {
            "Informe de 1 a 1440 minutos ou deixe vazio."
        }

        formularioValido = duracaoValida && jornadaValida

        if (!formularioValido) {
            atualizarStatus()
            return false
        }

        modelo.atualizar(
            nome = nome.texto.text.toString(),
            descricao = descricao.texto.text.toString(),
            duracaoMinutos = minutosAtividade,
            jornadaMinutos = minutosJornada,
            recuperacao = recuperacaoSelecionada,
            distribuicaoPausas = distribuicao.texto.text.toString()
        )

        atualizarStatus()
        return true
    }

    private fun atualizarBotaoPausas() {
        botaoPausas.text = textoOpcao(
            "Pausas e recuperação",
            opcoesPausas,
            recuperacaoSelecionada?.toDouble()
        )
    }

    private fun salvarESair() {
        if (saindo) return

        if (!carregado) {
            finish()
            return
        }

        modelo.interromperMedicao()

        if (!atualizarFormulario()) {
            Toast.makeText(
                this,
                "Corrija o valor inválido ou deixe o campo vazio.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        saindo = true
        habilitarFormulario(false)

        lifecycleScope.launch {
            try {
                if (modelo.salvarAgora()) {
                    finish()
                }
            } finally {
                saindo = false

                if (!isFinishing) {
                    habilitarFormulario(true)
                }
            }
        }
    }

    private fun habilitarFormulario(habilitar: Boolean) {
        listOf(
            nome,
            descricao,
            duracao,
            jornada,
            distribuicao
        ).forEach {
            it.texto.isEnabled = habilitar
        }

        botaoPausas.isEnabled = habilitar
        botaoSalvar.isEnabled = habilitar
        botaoVoltar.isEnabled = habilitar

        modelo.checklistVisivel.value?.let {
            atualizarPaineis(it)
        }

        if (!habilitar) {
            paineis.values.forEach {
                it.iniciar.isEnabled = false
                it.adicionar.isEnabled = false
                it.desfazer.isEnabled = false
                it.interromper.isEnabled = false
                it.dinamica.isEnabled = false
                it.estatica.isEnabled = false
            }
        }
    }

    private fun adicionarCampo(
        titulo: String,
        numerico: Boolean = false,
        multiline: Boolean = false
    ): Campo {
        val layout = TextInputLayout(this).apply {
            hint = titulo

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        val campo = TextInputEditText(layout.context).apply {
            isSaveEnabled = false

            inputType = when {
                numerico -> InputType.TYPE_CLASS_NUMBER

                multiline -> InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE

                else -> InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }

            if (multiline) {
                minLines = 3
                gravity = Gravity.TOP
            } else {
                setSingleLine(true)
            }

            setOnFocusChangeListener { _, temFoco ->
                if (temFoco) {
                    rolagem.post {
                        mostrarCampoEmFoco()
                    }
                }
            }
        }

        layout.addView(campo)
        conteudo.addView(layout)
        return Campo(layout, campo)
    }

    private fun adicionarTexto(texto: String): TextView {
        val campo = TextView(this).apply {
            text = texto
            textSize = 14f

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        conteudo.addView(campo)
        return campo
    }

    private fun adicionarTitulo(texto: String) {
        adicionarTexto(texto).apply {
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(8))
        }
    }

    private fun adicionarBotao(
        texto: String,
        acao: () -> Unit
    ): MaterialButton {
        val botao = MaterialButton(this).apply {
            text = texto
            isAllCaps = false
            minHeight = dp(56)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }

            setOnClickListener {
                acao()
            }
        }

        conteudo.addView(botao)
        return botao
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_ATIVIDADE_ID = "atividade_id"
    }
}