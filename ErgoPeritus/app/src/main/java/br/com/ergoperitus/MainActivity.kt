package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import br.com.ergoperitus.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val preferencias by lazy {
        getSharedPreferences(
            "seguranca_ergo_peritus",
            MODE_PRIVATE
        )
    }

    /*
     * Abre o cadastro e aguarda o usuário concluí-lo.
     */
    private val abrirTelaCadastro = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->

        if (resultado.resultCode == RESULT_OK) {
            val emailCadastrado = resultado.data
                ?.getStringExtra("email_cadastrado")

            binding.etEmail.setText(emailCadastrado)

            Toast.makeText(
                this,
                "Cadastro concluído. Digite sua senha para entrar.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBiometria()
        configurarEventos()
        verificarBiometriaAoAbrir()
    }

    private fun configurarEventos() {

        binding.btnEntrar.setOnClickListener {
            validarLogin()
        }

        binding.tvCadastrar.setOnClickListener {
            val intent = Intent(
                this,
                CadastroActivity::class.java
            )

            abrirTelaCadastro.launch(intent)
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "A recuperação de senha será implementada posteriormente.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun configurarBiometria() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    Toast.makeText(
                        this@MainActivity,
                        "Digital reconhecida.",
                        Toast.LENGTH_SHORT
                    ).show()

                    entrarNoAplicativo()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Toast.makeText(
                        this@MainActivity,
                        "Digital não reconhecida. Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(
                        errorCode,
                        errString
                    )

                    if (
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            errString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Entrar no Ergo Peritus")
            .setSubtitle("Use sua digital para continuar")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .setNegativeButtonText("Usar e-mail e senha")
            .build()
    }

    private fun verificarBiometriaAoAbrir() {
        val biometriaAtivada = preferencias.getBoolean(
            "biometria_ativada",
            false
        )

        if (!biometriaAtivada) {
            return
        }

        val biometricManager = BiometricManager.from(this)

        val resultado = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (resultado == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun validarLogin() {
        val email = binding.etEmail.text
            .toString()
            .trim()

        val senha = binding.etPassword.text
            .toString()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var formularioValido = true

        if (email.isEmpty()) {
            binding.tilEmail.error =
                getString(R.string.email_required)

            formularioValido = false

        } else if (
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            binding.tilEmail.error =
                getString(R.string.email_invalid)

            formularioValido = false
        }

        if (senha.isEmpty()) {
            binding.tilPassword.error =
                getString(R.string.password_required)

            formularioValido = false

        } else if (senha.length < 6) {
            binding.tilPassword.error =
                getString(R.string.password_short)

            formularioValido = false
        }

        if (formularioValido) {
            realizarLogin(email, senha)
        }
    }

    private fun realizarLogin(
        email: String,
        senha: String
    ) {
        /*
         * O login ainda é uma simulação.
         * Depois será substituído pelo serviço de autenticação.
         */
        if (email.isNotEmpty() && senha.isNotEmpty()) {
            oferecerAtivacaoDaBiometria()
        }
    }

    private fun oferecerAtivacaoDaBiometria() {
        val biometriaJaAtivada = preferencias.getBoolean(
            "biometria_ativada",
            false
        )

        if (biometriaJaAtivada) {
            entrarNoAplicativo()
            return
        }

        val biometricManager = BiometricManager.from(this)

        val resultado = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (resultado != BiometricManager.BIOMETRIC_SUCCESS) {
            entrarNoAplicativo()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Ativar entrada por digital?")
            .setMessage(
                "Nos próximos acessos, você poderá entrar sem digitar seu e-mail e senha."
            )
            .setPositiveButton("Ativar") { _, _ ->

                preferencias.edit()
                    .putBoolean(
                        "biometria_ativada",
                        true
                    )
                    .apply()

                entrarNoAplicativo()
            }
            .setNegativeButton("Agora não") { _, _ ->
                entrarNoAplicativo()
            }
            .show()
    }

    /*
     * Abre o HUB depois do login com senha
     * ou da autenticação biométrica.
     */
    private fun entrarNoAplicativo() {
        val intent = Intent(
            this,
            HubActivity::class.java
        )

        startActivity(intent)

        /*
         * Fecha a tela de login para que o botão Voltar
         * não retorne ao login depois da autenticação.
         */
        finish()
    }
}