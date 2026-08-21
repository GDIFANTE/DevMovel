package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.ergoperitus.databinding.ActivityCadastroBinding

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Monta a tela definida no arquivo
         * activity_cadastro.xml.
         */
        binding = ActivityCadastroBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        configurarEventos()
    }

    private fun configurarEventos() {

        /*
         * Valida os campos quando o botão
         * Cadastrar for pressionado.
         */
        binding.btnCadastrar.setOnClickListener {
            validarCadastro()
        }

        /*
         * Fecha o cadastro e retorna para o login.
         */
        binding.tvVoltarLogin.setOnClickListener {
            finish()
        }
    }

    private fun validarCadastro() {
        val nome = binding.etNome.text
            .toString()
            .trim()

        val email = binding.etEmailCadastro.text
            .toString()
            .trim()

        val senha = binding.etSenhaCadastro.text
            .toString()

        val confirmarSenha = binding.etConfirmarSenha.text
            .toString()

        /*
         * Apaga mensagens de erro anteriores.
         */
        binding.tilNome.error = null
        binding.tilEmailCadastro.error = null
        binding.tilSenhaCadastro.error = null
        binding.tilConfirmarSenha.error = null

        var formularioValido = true

        if (nome.isEmpty()) {
            binding.tilNome.error =
                getString(R.string.name_required)

            formularioValido = false

        } else if (nome.length < 3) {
            binding.tilNome.error =
                getString(R.string.name_short)

            formularioValido = false
        }

        if (email.isEmpty()) {
            binding.tilEmailCadastro.error =
                getString(R.string.email_required)

            formularioValido = false

        } else if (
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            binding.tilEmailCadastro.error =
                getString(R.string.email_invalid)

            formularioValido = false
        }

        if (senha.isEmpty()) {
            binding.tilSenhaCadastro.error =
                getString(R.string.password_required)

            formularioValido = false

        } else if (senha.length < 6) {
            binding.tilSenhaCadastro.error =
                getString(R.string.password_short)

            formularioValido = false
        }

        if (confirmarSenha.isEmpty()) {
            binding.tilConfirmarSenha.error =
                getString(
                    R.string.confirm_password_required
                )

            formularioValido = false

        } else if (senha != confirmarSenha) {
            binding.tilConfirmarSenha.error =
                getString(R.string.passwords_different)

            formularioValido = false
        }

        if (formularioValido) {
            concluirCadastro(nome, email)
        }
    }

    private fun concluirCadastro(
        nome: String,
        email: String
    ) {
        /*
         * Por enquanto, este método apenas conclui
         * a validação visual.
         *
         * Posteriormente enviaremos nome, e-mail e senha
         * para o Firebase ou para uma API.
         */

        Toast.makeText(
            this,
            "Cadastro de $nome realizado com sucesso.",
            Toast.LENGTH_SHORT
        ).show()

        /*
         * Devolve o e-mail para a tela de login.
         */
        val resultado = Intent().apply {
            putExtra("email_cadastrado", email)
        }

        setResult(RESULT_OK, resultado)

        /*
         * Fecha a tela de cadastro.
         */
        finish()
    }
}