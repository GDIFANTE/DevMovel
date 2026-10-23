package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ResultadoOcraActivity : AppCompatActivity() {

    private val banco by lazy {
        ErgoPeritusDatabase.obter(applicationContext)
    }

    private lateinit var conteudo: LinearLayout

    private var atividadeId = 0L
    private var usuarioId = 0L
    private var carregando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = SessaoUsuario(applicationContext).usuarioId()

        if (id == null) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        usuarioId = id
        atividadeId = intent.getLongExtra(EXTRA_ATIVIDADE_ID, 0L)

        if (atividadeId <= 0L) {
            finish()
            return
        }

        montarTela()
        carregar()
    }

    private fun montarTela() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        val rolagem = ScrollView(this).apply {
            isFillViewport = true
            addView(conteudo)
        }

        setContentView(rolagem)

        ViewCompat.setOnApplyWindowInsetsListener(rolagem) { view, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.requestApplyInsets(rolagem)
        texto("Carregando resultado...")
    }

    private fun carregar() {
        if (carregando) return
        carregando = true

        lifecycleScope.launch {
            try {
                // Lê os blocos na mesma transação para montar
                // um retrato consistente das respostas salvas.
                val dados = banco.withTransaction {
                    val atividade = banco.atividadeDao().buscarPorId(
                        atividadeId,
                        usuarioId
                    ) ?: error("Atividade não encontrada.")

                    val checklist = banco.checklistOcraDao()
                        .buscarPorAtividade(
                            atividadeId,
                            usuarioId
                        )

                    val resultados = listOf("D", "E").map { lado ->
                        lado to CalculadoraOcra.calcular(
                            atividade = atividade,
                            checklist = checklist,
                            forca = banco.forcaOcraDao().buscar(
                                atividadeId,
                                lado,
                                usuarioId
                            ),
                            postura = banco.posturaOcraDao().buscar(
                                atividadeId,
                                lado,
                                usuarioId
                            ),
                            complementares =
                                banco.complementaresOcraDao().buscar(
                                    atividadeId,
                                    lado,
                                    usuarioId
                                ),
                            lado = lado
                        )
                    }

                    atividade to resultados
                }

                conteudo.removeAllViews()

                titulo("Resultado do Checklist OCRA")
                texto("Atividade: ${dados.first.nome}")

                texto(
                    "Resultado desta atividade isolada. Quando houver " +
                            "várias tarefas repetitivas no turno, este " +
                            "resultado não representa a exposição " +
                            "combinada da jornada."
                )

                texto(
                    "As pontuações são calculadas com as respostas " +
                            "salvas. A classificação usa uma casa decimal, " +
                            "com arredondamento convencional."
                )

                dados.second.forEach { (lado, resultado) ->
                    mostrarResultado(lado, resultado)
                }

                botao("Atualizar resultado") {
                    carregar()
                }

                botao("Voltar às atividades") {
                    finish()
                }

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                conteudo.removeAllViews()
                titulo("Não foi possível carregar o resultado")

                botao("Tentar novamente") {
                    carregar()
                }

                botao("Voltar") {
                    finish()
                }

                Toast.makeText(
                    this@ResultadoOcraActivity,
                    "Confira a atividade e tente novamente.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                carregando = false
            }
        }
    }

    private fun mostrarResultado(
        lado: String,
        resultado: CalculadoraOcra.Resultado
    ) {
        titulo(
            if (lado == "D") "Braço direito" else "Braço esquerdo"
        )

        texto(
            "Recuperação: ${formatar(resultado.recuperacao)}\n" +
                    "Frequência: ${formatar(resultado.frequencia)}\n" +
                    "Força: ${formatar(resultado.forca)}\n" +
                    "Postura: ${formatar(resultado.postura)}\n" +
                    "Complementares: ${formatar(resultado.complementares)}"
        )

        texto(
            "Soma dos cinco blocos: " +
                    formatar(resultado.pontuacaoIntrinseca) +
                    "\nDuração diária da atividade: " +
                    (
                            resultado.duracaoMinutos?.let {
                                "$it minutos"
                            } ?: "não informada"
                            ) +
                    "\nMultiplicador: " +
                    formatar(resultado.multiplicador)
        )

        if (resultado.pendencias.isNotEmpty()) {
            texto(
                "RESULTADO PENDENTE",
                20f,
                true
            )

            texto(
                resultado.pendencias.joinToString("\n\n") {
                    "• $it"
                }
            )
        } else {
            texto(
                "Pontuação ponderada: " +
                        formatar(resultado.pontuacaoPonderada),
                22f,
                true
            )

            resultado.pontuacaoPonderada?.let {
                texto(
                    "Valor para classificação: " +
                            formatar(
                                CalculadoraOcra.arredondarUmaCasa(it)
                            )
                )
            }

            texto(
                resultado.classificacao.orEmpty(),
                20f,
                true
            )

            texto(
                "Cálculo: " +
                        formatar(resultado.pontuacaoIntrinseca) +
                        " × " +
                        formatar(resultado.multiplicador) +
                        " = " +
                        formatar(resultado.pontuacaoPonderada)
            )
        }

        resultado.observacoes.forEach {
            texto("Observação: $it", 16f, true)
        }
    }

    private fun formatar(valor: Double?): String {
        if (valor == null) return "pendente"

        return BigDecimal.valueOf(valor)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
    }

    private fun texto(
        valor: String,
        tamanho: Float = 16f,
        negrito: Boolean = false
    ) {
        conteudo.addView(
            TextView(this).apply {
                text = valor
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
                    topMargin = dp(16)
                }
            }
        )
    }

    private fun titulo(valor: String) {
        texto(valor, 24f, true)
    }

    private fun botao(
        valor: String,
        acao: () -> Unit
    ) {
        conteudo.addView(
            MaterialButton(this).apply {
                text = valor
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
        )
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_ATIVIDADE_ID = "atividade_id"
    }
}