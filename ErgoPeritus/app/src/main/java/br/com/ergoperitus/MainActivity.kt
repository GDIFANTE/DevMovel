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
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.dados.Usuario
import br.com.ergoperitus.databinding.ActivityMainBinding
import br.com.ergoperitus.seguranca.ProtecaoSenha
import br.com.ergoperitus.seguranca.SessaoUsuario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var sessao: SessaoUsuario

    private val usuarioDao by lazy {
        ErgoPeritusDatabase.obter(applicationContext).usuarioDao()
    }

    private val abrirTelaCadastro = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val emailCadastrado = resultado.data
                ?.getStringExtra(CadastroActivity.EXTRA_EMAIL_CADASTRADO)

            binding.etEmail.setText(emailCadastrado)
            binding.etPassword.requestFocus()

            Toast.makeText(
                this,
                R.string.register_complete_login,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessao = SessaoUsuario(applicationContext)

        configurarBiometria()
        configurarEventos()
        verificarBiometriaAoAbrir()
    }

    private fun configurarEventos() {
        binding.btnEntrar.setOnClickListener {
            validarFormularioLogin()
        }

        binding.tvCadastrar.setOnClickListener {
            abrirTelaCadastro.launch(
                Intent(this, CadastroActivity::class.java)
            )
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                R.string.password_recovery_later,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun validarFormularioLogin() {
        val email = binding.etEmail.text
            .toString()
            .trim()
            .lowercase(Locale.ROOT)
        val senha = binding.etPassword.text.toString()

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
        binding.btnEntrar.isEnabled = false

        lifecycleScope.launch {
            try {
                val usuario = usuarioDao.buscarPorEmail(email)

                val credenciaisCorretas = usuario != null &&
                    withContext(Dispatchers.Default) {
                        ProtecaoSenha.corresponde(
                            senhaInformada = senha.toCharArray(),
                            hashSalvo = usuario.senhaHash,
                            saltSalvo = usuario.senhaSalt
                        )
                    }

                if (!credenciaisCorretas || usuario == null) {
                    // A mensagem genérica não revela se o e-mail existe.
                    binding.tilPassword.error =
                        getString(R.string.invalid_credentials)
                    return@launch
                }

                loginConfirmado(usuario)
            } catch (_: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    R.string.login_error,
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnEntrar.isEnabled = true
            }
        }
    }

    private fun loginConfirmado(usuario: Usuario) {
        sessao.registrarLogin(usuario.id)
        oferecerAtivacaoDaBiometria()
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
                        R.string.biometric_success,
                        Toast.LENGTH_SHORT
                    ).show()

                    validarSessaoBiometrica()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@MainActivity,
                        R.string.biometric_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

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
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .setNegativeButtonText(getString(R.string.use_email_password))
            .build()
    }

    private fun verificarBiometriaAoAbrir() {
        val existeUsuarioVinculado = sessao.usuarioId() != null

        if (!sessao.biometriaAtivada() || !existeUsuarioVinculado) {
            return
        }

        val resultado = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (resultado == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun validarSessaoBiometrica() {
        val usuarioId = sessao.usuarioId()

        if (usuarioId == null) {
            sessao.encerrar()
            return
        }

        lifecycleScope.launch {
            val usuarioAindaExiste = try {
                usuarioDao.buscarPorId(usuarioId) != null
            } catch (_: Exception) {
                false
            }

            if (usuarioAindaExiste) {
                entrarNoAplicativo()
            } else {
                // Se banco e preferências ficarem inconsistentes, o acesso
                // volta ao login em vez de liberar o HUB indevidamente.
                sessao.encerrar()
                Toast.makeText(
                    this@MainActivity,
                    R.string.session_invalid,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun oferecerAtivacaoDaBiometria() {
        if (sessao.biometriaAtivada()) {
            entrarNoAplicativo()
            return
        }

        val resultado = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (resultado != BiometricManager.BIOMETRIC_SUCCESS) {
            entrarNoAplicativo()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.enable_biometric_title)
            .setMessage(R.string.enable_biometric_message)
            .setPositiveButton(R.string.enable) { _, _ ->
                sessao.ativarBiometria()
                entrarNoAplicativo()
            }
            .setNegativeButton(R.string.not_now) { _, _ ->
                entrarNoAplicativo()
            }
            .show()
    }

    private fun entrarNoAplicativo() {
        startActivity(Intent(this, HubActivity::class.java))
        finish()
    }
}
