package br.com.ergoperitus

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ergoperitus.dados.ErgoPeritusDatabase
import br.com.ergoperitus.databinding.ActivityRecuperarSenhaBinding
import br.com.ergoperitus.seguranca.CodigoRecuperacao
import br.com.ergoperitus.seguranca.ProtecaoSenha
import br.com.ergoperitus.seguranca.SessaoUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RecuperarSenhaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecuperarSenhaBinding

    private val usuarioDao by lazy {
        ErgoPeritusDatabase.obter(applicationContext).usuarioDao()
    }

    private val sessao by lazy {
        SessaoUsuario(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecuperarSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnRedefinir.setOnClickListener {
            validarFormulario()
        }
    }

    private fun validarFormulario() {
        val email = binding.etEmail.text.toString()
            .trim()
            .lowercase(Locale.ROOT)

        val codigo = CodigoRecuperacao.normalizar(
            binding.etCodigo.text.toString()
        )

        val novaSenha = binding.etNovaSenha.text.toString()
        val confirmarSenha = binding.etConfirmarSenha.text.toString()

        limparErros()

        var valido = true

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Informe um e-mail válido."
            valido = false
        }

        if (!CodigoRecuperacao.formatoValido(codigo)) {
            binding.tilCodigo.error =
                "Informe o código completo de 32 letras e números."
            valido = false
        }

        if (novaSenha.length < 6) {
            binding.tilNovaSenha.error =
                "A nova senha deve ter pelo menos 6 caracteres."
            valido = false
        }

        if (confirmarSenha.isEmpty() || novaSenha != confirmarSenha) {
            binding.tilConfirmarSenha.error =
                "A confirmação deve ser igual à nova senha."
            valido = false
        }

        if (valido) {
            redefinirSenha(email, codigo, novaSenha)
        }
    }

    private fun redefinirSenha(
        email: String,
        codigo: String,
        novaSenha: String
    ) {
        binding.btnRedefinir.isEnabled = false
        binding.btnVoltar.isEnabled = false

        lifecycleScope.launch {
            try {
                val usuario = usuarioDao.buscarPorEmail(email)
                val codigoHash = usuario?.recuperacaoHash
                val codigoSalt = usuario?.recuperacaoSalt

                if (
                    usuario == null ||
                    codigoHash == null ||
                    codigoSalt == null
                ) {
                    informarCodigoInvalido()
                    return@launch
                }

                val codigoCorreto = withContext(Dispatchers.Default) {
                    ProtecaoSenha.corresponde(
                        senhaInformada = codigo.toCharArray(),
                        hashSalvo = codigoHash,
                        saltSalvo = codigoSalt
                    )
                }

                if (!codigoCorreto) {
                    informarCodigoInvalido()
                    return@launch
                }

                val senhaProtegida = withContext(Dispatchers.Default) {
                    ProtecaoSenha.proteger(novaSenha.toCharArray())
                }

                // Troca a senha e consome o código na mesma operação.
                val registrosAlterados = usuarioDao.redefinirSenhaComCodigo(
                    usuarioId = usuario.id,
                    codigoHash = codigoHash,
                    codigoSalt = codigoSalt,
                    novaSenhaHash = senhaProtegida.hash,
                    novaSenhaSalt = senhaProtegida.salt
                )

                if (registrosAlterados != 1) {
                    informarCodigoInvalido()
                    return@launch
                }

                // Exige um novo login e remove o vínculo da biometria.
                sessao.encerrar()

                binding.etCodigo.text?.clear()
                binding.etNovaSenha.text?.clear()
                binding.etConfirmarSenha.text?.clear()

                Toast.makeText(
                    this@RecuperarSenhaActivity,
                    "Senha redefinida! Entre com a nova senha. " +
                            "Depois gere um novo código em Meu cadastro.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(
                    this@RecuperarSenhaActivity,
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                Toast.makeText(
                    this@RecuperarSenhaActivity,
                    "Não foi possível redefinir a senha. Tente novamente.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnRedefinir.isEnabled = true
                binding.btnVoltar.isEnabled = true
            }
        }
    }

    private fun informarCodigoInvalido() {
        binding.tilCodigo.error =
            "E-mail ou código inválido, ou código já utilizado."
    }

    private fun limparErros() {
        binding.tilEmail.error = null
        binding.tilCodigo.error = null
        binding.tilNovaSenha.error = null
        binding.tilConfirmarSenha.error = null
    }
}