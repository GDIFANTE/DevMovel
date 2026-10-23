package br.com.ergoperitus

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.dados.Evento
import br.com.ergoperitus.databinding.ActivityCadastroEventoBinding
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CadastroEventoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroEventoBinding

    private val banco by lazy {
        ErgoPeritusDatabase.obter(applicationContext)
    }

    private val sessao by lazy {
        SessaoUsuario(applicationContext)
    }

    private var usuarioId: Long? = null
    private var dataSelecionada: String? = null
    private var salvando = false

    // Permite tentar abrir novamente sem cadastrar outra capa.
    private var eventoSalvoId: Long? = null

    private data class CampoFuncao(
        val linha: LinearLayout,
        val layout: TextInputLayout,
        val campo: TextInputEditText,
        val remover: MaterialButton
    )

    private val camposFuncoes = mutableListOf<CampoFuncao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCadastroEventoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventoSalvoId = savedInstanceState
            ?.getLong("evento_salvo_id")
            ?.takeIf { it > 0L }

        binding.btnAdicionarFuncao.setOnClickListener {
            adicionarFuncao()
        }

        binding.etData.setOnClickListener {
            escolherData()
        }

        binding.btnSalvarEvento.setOnClickListener {
            validarESalvar()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        val funcoesSalvas = savedInstanceState
            ?.getStringArrayList("funcoes")

        if (funcoesSalvas.isNullOrEmpty()) {
            adicionarFuncao()
        } else {
            funcoesSalvas.forEach {
                adicionarFuncao(it)
            }
        }

        dataSelecionada = savedInstanceState?.getString("data")
        atualizarDataNaTela()

        if (eventoSalvoId != null) {
            bloquearCapaSalva()
        }

        verificarUsuario()
    }

    private fun verificarUsuario() {
        val id = sessao.usuarioId()

        if (id == null) {
            voltarAoLogin()
            return
        }

        lifecycleScope.launch {
            try {
                if (banco.usuarioDao().buscarPorId(id) == null) {
                    voltarAoLogin()
                    return@launch
                }

                usuarioId = id
                binding.btnSalvarEvento.isEnabled = true

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                mostrarMensagem("Não foi possível carregar o usuário.")
                finish()
            }
        }
    }

    private fun adicionarFuncao(nome: String = "") {
        if (salvando) return

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val layout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val campo = TextInputEditText(layout.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

            setSingleLine(true)
            isSaveEnabled = false
            setText(nome)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val remover = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Remover função"

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(campo)
        linha.addView(layout)
        linha.addView(remover)

        val item = CampoFuncao(linha, layout, campo, remover)

        remover.setOnClickListener {
            if (
                !salvando &&
                eventoSalvoId == null &&
                camposFuncoes.size > 1
            ) {
                camposFuncoes.remove(item)
                binding.containerFuncoes.removeView(linha)
                atualizarRotulosFuncoes()
            }
        }

        camposFuncoes.add(item)
        binding.containerFuncoes.addView(linha)
        atualizarRotulosFuncoes()
    }

    private fun atualizarRotulosFuncoes() {
        camposFuncoes.forEachIndexed { indice, item ->
            item.layout.hint = "Função ${indice + 1}"

            item.remover.visibility =
                if (camposFuncoes.size > 1) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    private fun escolherData() {
        if (salvando || eventoSalvoId != null) return

        val calendario = Calendar.getInstance()

        dataSelecionada?.split("-")?.let { partes ->
            calendario.set(
                partes[0].toInt(),
                partes[1].toInt() - 1,
                partes[2].toInt()
            )
        }

        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                dataSelecionada = String.format(
                    Locale.ROOT,
                    "%04d-%02d-%02d",
                    ano,
                    mes + 1,
                    dia
                )

                binding.tilData.error = null
                atualizarDataNaTela()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun atualizarDataNaTela() {
        val data = dataSelecionada ?: return
        val partes = data.split("-")

        binding.etData.setText(
            "${partes[2]}/${partes[1]}/${partes[0]}"
        )
    }

    private fun validarESalvar() {
        if (salvando) return

        val id = usuarioId ?: return

        eventoSalvoId?.let {
            abrirAvaliacao(it)
            return
        }

        val processo = binding.etProcesso.text.toString().trim()
        val vara = binding.etVara.text.toString().trim()
        val reclamante = binding.etReclamante.text.toString().trim()
        val reclamada = binding.etReclamada.text.toString().trim()
        val data = dataSelecionada

        binding.tilProcesso.error = null
        binding.tilVara.error = null
        binding.tilReclamante.error = null
        binding.tilReclamada.error = null
        binding.tilData.error = null

        var valido = true

        if (processo.isEmpty()) {
            binding.tilProcesso.error = "Informe o processo."
            valido = false
        }

        if (vara.isEmpty()) {
            binding.tilVara.error = "Informe a vara."
            valido = false
        }

        if (reclamante.isEmpty()) {
            binding.tilReclamante.error = "Informe o reclamante."
            valido = false
        }

        if (reclamada.isEmpty()) {
            binding.tilReclamada.error = "Informe a reclamada."
            valido = false
        }

        val funcoes = camposFuncoes.map { item ->
            item.layout.error = null
            val nome = item.campo.text.toString().trim()

            if (nome.isEmpty()) {
                item.layout.error =
                    "Informe a função ou remova este campo."
                valido = false
            }

            nome
        }

        if (data == null) {
            binding.tilData.error = "Selecione a data."
            valido = false
        }

        if (!valido || data == null) return

        val evento = Evento(
            usuarioId = id,
            processo = processo,
            vara = vara,
            reclamante = reclamante,
            reclamada = reclamada,
            data = data
        )

        definirSalvando(true)

        lifecycleScope.launch {
            try {
                val eventoId = banco.eventoDao()
                    .salvarComFuncoes(evento, funcoes)

                eventoSalvoId = eventoId
                bloquearCapaSalva()

                mostrarMensagem("Capa da avaliação cadastrada.")
                setResult(RESULT_OK)

                abrirAvaliacao(eventoId)

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                mostrarMensagem(
                    "Não foi possível salvar a capa. Tente novamente."
                )
            } finally {
                definirSalvando(false)
            }
        }
    }

    private fun abrirAvaliacao(eventoId: Long) {
        try {
            startActivity(
                Intent(this, AvaliacaoActivity::class.java).apply {
                    putExtra(
                        AvaliacaoActivity.EXTRA_EVENTO_ID,
                        eventoId
                    )
                }
            )

            finish()

        } catch (_: Exception) {
            mostrarMensagem(
                "A capa está salva, mas não foi possível abrir " +
                        "as atividades. Tente novamente."
            )
        }
    }

    private fun bloquearCapaSalva() {
        binding.btnSalvarEvento.text = "Abrir atividades"

        binding.etProcesso.isEnabled = false
        binding.etVara.isEnabled = false
        binding.etReclamante.isEnabled = false
        binding.etReclamada.isEnabled = false
        binding.etData.isEnabled = false

        binding.btnAdicionarFuncao.isEnabled = false

        camposFuncoes.forEach {
            it.campo.isEnabled = false
            it.remover.isEnabled = false
        }
    }

    private fun definirSalvando(valor: Boolean) {
        salvando = valor
        val editavel = !valor && eventoSalvoId == null

        binding.btnSalvarEvento.isEnabled =
            !valor && usuarioId != null

        binding.btnAdicionarFuncao.isEnabled = editavel
        binding.btnVoltar.isEnabled = !valor

        binding.etProcesso.isEnabled = editavel
        binding.etVara.isEnabled = editavel
        binding.etReclamante.isEnabled = editavel
        binding.etReclamada.isEnabled = editavel
        binding.etData.isEnabled = editavel

        camposFuncoes.forEach {
            it.campo.isEnabled = editavel
            it.remover.isEnabled = editavel
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(
            "funcoes",
            ArrayList(
                camposFuncoes.map {
                    it.campo.text.toString()
                }
            )
        )

        outState.putString("data", dataSelecionada)

        eventoSalvoId?.let {
            outState.putLong("evento_salvo_id", it)
        }

        super.onSaveInstanceState(outState)
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
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
}