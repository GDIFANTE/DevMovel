package br.com.ergoperitus

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.ergoperitus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    /*
     * O binding fornece acesso aos componentes do XML.
     * Por exemplo: btnEntrar no XML vira binding.btnEntrar no Kotlin.
     */
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Constroi a tela definida no arquivo activity_main.xml.
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Exibe a tela no celular.
        setContentView(binding.root)

        configurarEventos()
    }

    private fun configurarEventos() {

        binding.btnEntrar.setOnClickListener {
            validarLogin()
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "A recuperação de senha será implementada na próxima etapa.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun validarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etPassword.text.toString()

        // Apaga mensagens de erro anteriores.
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var formularioValido = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            formularioValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.email_invalid)
            formularioValido = false
        }

        if (senha.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            formularioValido = false
        } else if (senha.length < 6) {
            binding.tilPassword.error = getString(R.string.password_short)
            formularioValido = false
        }

        if (formularioValido) {
            realizarLogin(email, senha)
        }
    }

    private fun realizarLogin(email: String, senha: String) {

        /*
         * Posteriormente esta função poderá chamar uma API
         * ou um serviço como o Firebase Authentication.
         * preciso definir banco de dados e como salvar no aparelho e se vou fazer upload para nuvem
         */

        Toast.makeText(
            this,
            "Dados preenchidos corretamente.",
            Toast.LENGTH_SHORT
        ).show()
    }
}