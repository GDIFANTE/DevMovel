package br.com.ergoperitus

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.dados.Usuario
import br.com.ergoperitus.databinding.ActivityCadastroBinding
import br.com.ergoperitus.seguranca.ProtecaoSenha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding

    private val usuarioDao by lazy {
        ErgoPeritusDatabase.obter(applicationContext).usuarioDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnCadastrar.setOnClickListener {
            validarCadastro()
        }

        binding.tvVoltarLogin.setOnClickListener {
            finish()
        }
    }

    private fun validarCadastro() {
        val nome = binding.etNome.text.toString().trim()
        val email = binding.etEmailCadastro.text
            .toString()
            .trim()
            .lowercase(Locale.ROOT)
        val senha = binding.etSenhaCadastro.text.toString()
        val confirmarSenha = binding.etConfirmarSenha.text.toString()

        limparErros()

        var formularioValido = true

        if (nome.isEmpty()) {
            binding.tilNome.error = getString(R.string.name_required)
            formularioValido = false
        } else if (nome.length < 3) {
            binding.tilNome.error = getString(R.string.name_short)
            formularioValido = false
        }

        if (email.isEmpty()) {
            binding.tilEmailCadastro.error = getString(R.string.email_required)
            formularioValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmailCadastro.error = getString(R.string.email_invalid)
            formularioValido = false
        }

        if (senha.isEmpty()) {
            binding.tilSenhaCadastro.error = getString(R.string.password_required)
            formularioValido = false
        } else if (senha.length < 6) {
            binding.tilSenhaCadastro.error = getString(R.string.password_short)
            formularioValido = false
        }

        if (confirmarSenha.isEmpty()) {
            binding.tilConfirmarSenha.error =
                getString(R.string.confirm_password_required)
            formularioValido = false
        } else if (senha != confirmarSenha) {
            binding.tilConfirmarSenha.error =
                getString(R.string.passwords_different)
            formularioValido = false
        }

        if (formularioValido) {
            salvarUsuario(nome, email, senha)
        }
    }

    private fun limparErros() {
        binding.tilNome.error = null
        binding.tilEmailCadastro.error = null
        binding.tilSenhaCadastro.error = null
        binding.tilConfirmarSenha.error = null
    }

    private fun salvarUsuario(
        nome: String,
        email: String,
        senha: String
    ) {
        // Evita dois toques seguidos enquanto o banco termina o cadastro.
        binding.btnCadastrar.isEnabled = false

        lifecycleScope.launch {
            try {
                if (usuarioDao.buscarPorEmail(email) != null) {
                    binding.tilEmailCadastro.error =
                        getString(R.string.email_already_registered)
                    return@launch
                }

                // O cálculo do hash é propositalmente trabalhoso. Executá-lo
                // fora da thread da tela evita a sensação de travamento.
                val senhaProtegida = withContext(Dispatchers.Default) {
                    ProtecaoSenha.proteger(senha.toCharArray())
                }

                usuarioDao.inserir(
                    Usuario(
                        nome = nome,
                        email = email,
                        senhaHash = senhaProtegida.hash,
                        senhaSalt = senhaProtegida.salt
                    )
                )

                concluirCadastro(nome, email)
            } catch (_: SQLiteConstraintException) {
                // Esta segunda proteção cobre dois cadastros quase simultâneos.
                binding.tilEmailCadastro.error =
                    getString(R.string.email_already_registered)
            } catch (_: Exception) {
                Toast.makeText(
                    this@CadastroActivity,
                    R.string.register_error,
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnCadastrar.isEnabled = true
            }
        }
    }

    private fun concluirCadastro(
        nome: String,
        email: String
    ) {
        Toast.makeText(
            this,
            getString(R.string.register_success, nome),
            Toast.LENGTH_SHORT
        ).show()

        val resultado = Intent().apply {
            putExtra(EXTRA_EMAIL_CADASTRADO, email)
        }

        setResult(RESULT_OK, resultado)
        finish()
    }

    companion object {
        const val EXTRA_EMAIL_CADASTRADO = "email_cadastrado"
    }
}
