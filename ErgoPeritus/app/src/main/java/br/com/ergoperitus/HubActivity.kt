package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.databinding.ActivityHubBinding
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class HubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHubBinding

    private val sessao by lazy {
        SessaoUsuario(applicationContext)
    }

    private val banco by lazy {
        ErgoPeritusDatabase.obter(applicationContext)
    }

    private var carregandoAvaliacoes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (sessao.usuarioId() == null) {
            sairDoAplicativo()
            return
        }

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnUsuario.setOnClickListener {
            mostrarMenuUsuario()
        }

        binding.btnNovaAvaliacao.setOnClickListener {
            startActivity(
                Intent(this, CadastroEventoActivity::class.java)
            )
        }

        binding.btnContinuarAvaliacao.setOnClickListener {
            mostrarAvaliacoes()
        }

        binding.btnHistorico.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.history_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarAvaliacoes() {
        if (carregandoAvaliacoes) return

        val usuarioId = sessao.usuarioId()

        if (usuarioId == null) {
            sairDoAplicativo()
            return
        }

        carregandoAvaliacoes = true
        binding.btnContinuarAvaliacao.isEnabled = false

        lifecycleScope.launch {
            try {
                val eventos = banco.eventoDao()
                    .listarPorUsuario(usuarioId)

                if (eventos.isEmpty()) {
                    Toast.makeText(
                        this@HubActivity,
                        "Nenhuma avaliação cadastrada. " +
                                "Comece em Nova avaliação.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                val opcoes = eventos.map { evento ->
                    "${evento.processo}\n" +
                            "${evento.reclamante} × ${evento.reclamada}"
                }.toTypedArray()

                MaterialAlertDialogBuilder(this@HubActivity)
                    .setTitle("Escolha a avaliação")
                    .setItems(opcoes) { _, posicao ->
                        val evento = eventos[posicao]

                        startActivity(
                            Intent(
                                this@HubActivity,
                                AvaliacaoActivity::class.java
                            ).apply {
                                putExtra(
                                    AvaliacaoActivity.EXTRA_EVENTO_ID,
                                    evento.id
                                )
                            }
                        )
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                Toast.makeText(
                    this@HubActivity,
                    "Não foi possível carregar as avaliações.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                carregandoAvaliacoes = false
                binding.btnContinuarAvaliacao.isEnabled = true
            }
        }
    }

    private fun mostrarMenuUsuario() {
        val menuUsuario = PopupMenu(
            this,
            binding.btnUsuario
        )

        menuUsuario.menuInflater.inflate(
            R.menu.menu_usuario,
            menuUsuario.menu
        )

        menuUsuario.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_meu_cadastro -> {
                    startActivity(
                        Intent(this, MeuCadastroActivity::class.java)
                    )
                    true
                }

                R.id.menu_sair -> {
                    confirmarSaida()
                    true
                }

                else -> false
            }
        }

        menuUsuario.show()
    }

    private fun confirmarSaida() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.logout_confirm) { _, _ ->
                sairDoAplicativo()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sairDoAplicativo() {
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