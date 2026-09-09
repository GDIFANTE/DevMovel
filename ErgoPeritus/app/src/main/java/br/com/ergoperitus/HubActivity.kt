package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import br.com.ergoperitus.databinding.ActivityHubBinding
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHubBinding

    private val sessao by lazy {
        SessaoUsuario(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Monta a interface definida em activity_hub.xml.
         */
        binding = ActivityHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarEventos()
    }

    private fun configurarEventos() {

        /*
         * Abre o pequeno menu ancorado no botão do usuário.
         */
        binding.btnUsuario.setOnClickListener {
            mostrarMenuUsuario()
        }

        /*
         * Os três botões já respondem ao toque.
         * As telas correspondentes serão criadas posteriormente.
         */
        binding.btnNovaAvaliacao.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.new_evaluation_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnContinuarAvaliacao.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.no_evaluation_in_progress),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnHistorico.setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.history_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarMenuUsuario() {
        val menuUsuario = PopupMenu(
            this,
            binding.btnUsuario
        )

        /*
         * Carrega as opções definidas em menu_usuario.xml.
         */
        menuUsuario.menuInflater.inflate(
            R.menu.menu_usuario,
            menuUsuario.menu
        )

        menuUsuario.setOnMenuItemClickListener { item ->

            when (item.itemId) {

                R.id.menu_meu_cadastro -> {
                    abrirMeuCadastro()
                    true
                }

                R.id.menu_sair -> {
                    confirmarSaida()
                    true
                }

                else -> false
            }
        }

        /*
         * Mostra o menu ao lado do botão do usuário.
         */
        menuUsuario.show()
    }

    private fun abrirMeuCadastro() {
        /*
         * Não reutilizamos a tela de criação de conta,
         * porque futuramente esta opção será uma tela de edição.
         */
        Toast.makeText(
            this,
            getString(R.string.profile_coming_soon),
            Toast.LENGTH_SHORT
        ).show()
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

        /*
         * Um logout completo desativa a biometria.
         * O usuário deverá utilizar e-mail e senha novamente.
         */
        sessao.encerrar()

        /*
         * Retorna para o login e remove as telas anteriores
         * da pilha de navegação.
         */
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}
