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
import br.com.ergoperitus.databinding.ActivityMeuCadastroBinding
import br.com.ergoperitus.seguranca.ProtecaoSenha
import br.com.ergoperitus.seguranca.SessaoUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.widget.TextView
import br.com.ergoperitus.seguranca.CodigoRecuperacao
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MeuCadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeuCadastroBinding

    private var usuarioCarregado: Usuario? = null

    private val sessao by lazy {
        SessaoUsuario(applicationContext)
    }

    private val usuarioDao by lazy {
        ErgoPeritusDatabase.obter(applicationContext).usuarioDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeuCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnSalvar.setOnClickListener {
            validarESalvar()
        }

        binding.btnGerarCodigo.setOnClickListener {
            confirmarGeracaoCodigo()
        }

        carregarUsuario()
    }

    private fun carregarUsuario() {
        val usuarioId = sessao.usuarioId()

        if (usuarioId == null) {
            voltarAoLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val usuario = usuarioDao.buscarPorId(usuarioId)

                if (usuario == null) {
                    voltarAoLogin()
                    return@launch
                }

                usuarioCarregado = usuario
                binding.etNome.setText(usuario.nome)
                binding.etEmail.setText(usuario.email)
                binding.btnSalvar.isEnabled = true
                binding.btnGerarCodigo.isEnabled = true

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                mostrarMensagem("Não foi possível carregar o cadastro.")
                finish()
            }
        }
    }

    private fun validarESalvar() {
        val usuario = usuarioCarregado ?: return

        val nome = binding.etNome.text.toString().trim()
        val email = binding.etEmail.text.toString()
            .trim()
            .lowercase(Locale.ROOT)

        val senhaAtual = binding.etSenhaAtual.text.toString()
        val novaSenha = binding.etNovaSenha.text.toString()
        val confirmarSenha = binding.etConfirmarSenha.text.toString()

        limparErros()

        var valido = true

        if (nome.length < 3) {
            binding.tilNome.error = "Informe um nome com pelo menos 3 caracteres."
            valido = false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Informe um e-mail válido."
            valido = false
        }

        if (senhaAtual.isEmpty()) {
            binding.tilSenhaAtual.error = "Informe sua senha atual."
            valido = false
        }

        val alterarSenha = novaSenha.isNotEmpty() ||
                confirmarSenha.isNotEmpty()

        if (alterarSenha) {
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
        }

        if (!valido) return

        binding.btnSalvar.isEnabled = false

        lifecycleScope.launch {
            try {
                // Confere a senha fora da thread que desenha a tela.
                val senhaCorreta = withContext(Dispatchers.Default) {
                    ProtecaoSenha.corresponde(
                        senhaInformada = senhaAtual.toCharArray(),
                        hashSalvo = usuario.senhaHash,
                        saltSalvo = usuario.senhaSalt
                    )
                }

                if (!senhaCorreta) {
                    binding.tilSenhaAtual.error = "Senha atual incorreta."
                    return@launch
                }

                val donoDoEmail = usuarioDao.buscarPorEmail(email)

                if (donoDoEmail != null && donoDoEmail.id != usuario.id) {
                    binding.tilEmail.error =
                        "Este e-mail já está cadastrado em outra conta."
                    return@launch
                }

                // copy mantém o ID e os demais dados do cadastro.
                var atualizado = usuario.copy(
                    nome = nome,
                    email = email
                )

                if (alterarSenha) {
                    val senhaProtegida = withContext(Dispatchers.Default) {
                        ProtecaoSenha.proteger(novaSenha.toCharArray())
                    }

                    atualizado = atualizado.copy(
                        senhaHash = senhaProtegida.hash,
                        senhaSalt = senhaProtegida.salt
                    )
                }

                val registrosAlterados = usuarioDao.atualizar(atualizado)

                if (registrosAlterados != 1) {
                    mostrarMensagem("Cadastro não encontrado. Entre novamente.")
                    voltarAoLogin()
                    return@launch
                }

                usuarioCarregado = atualizado

                binding.etSenhaAtual.text?.clear()
                binding.etNovaSenha.text?.clear()
                binding.etConfirmarSenha.text?.clear()

                mostrarMensagem("Cadastro atualizado com sucesso.")
                finish()

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: SQLiteConstraintException) {
                binding.tilEmail.error =
                    "Este e-mail já está cadastrado em outra conta."
            } catch (_: Exception) {
                mostrarMensagem("Não foi possível salvar. Tente novamente.")
            } finally {
                binding.btnSalvar.isEnabled = true
            }
        }
    }

    private fun limparErros() {
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilSenhaAtual.error = null
        binding.tilNovaSenha.error = null
        binding.tilConfirmarSenha.error = null
    }

    private fun mostrarMensagem(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }

    private fun voltarAoLogin() {
        sessao.encerrar()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
    private fun confirmarGeracaoCodigo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Gerar código de recuperação")
            .setMessage(
                "Informe sua senha atual no formulário antes de continuar. " +
                        "Ao gerar um novo código, o anterior deixará de funcionar."
            )
            .setPositiveButton("Gerar") { _, _ ->
                gerarCodigoRecuperacao()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun gerarCodigoRecuperacao() {
        val usuario = usuarioCarregado ?: return
        val senhaAtual = binding.etSenhaAtual.text.toString()

        binding.tilSenhaAtual.error = null

        if (senhaAtual.isEmpty()) {
            binding.tilSenhaAtual.error = "Informe sua senha atual."
            binding.etSenhaAtual.requestFocus()
            return
        }

        binding.btnSalvar.isEnabled = false
        binding.btnGerarCodigo.isEnabled = false

        lifecycleScope.launch {
            try {
                val senhaCorreta = withContext(Dispatchers.Default) {
                    ProtecaoSenha.corresponde(
                        senhaInformada = senhaAtual.toCharArray(),
                        hashSalvo = usuario.senhaHash,
                        saltSalvo = usuario.senhaSalt
                    )
                }

                if (!senhaCorreta) {
                    binding.tilSenhaAtual.error = "Senha atual incorreta."
                    return@launch
                }

                val codigo = CodigoRecuperacao.gerar()

                val codigoProtegido = withContext(Dispatchers.Default) {
                    ProtecaoSenha.proteger(
                        CodigoRecuperacao.normalizar(codigo).toCharArray()
                    )
                }

                val atualizado = usuario.copy(
                    recuperacaoHash = codigoProtegido.hash,
                    recuperacaoSalt = codigoProtegido.salt
                )

                if (usuarioDao.atualizar(atualizado) != 1) {
                    mostrarMensagem("Cadastro não encontrado. Entre novamente.")
                    voltarAoLogin()
                    return@launch
                }

                usuarioCarregado = atualizado
                binding.etSenhaAtual.text?.clear()

                mostrarCodigoRecuperacao(codigo)

            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                mostrarMensagem("Não foi possível gerar o código. Tente novamente.")
            } finally {
                binding.btnSalvar.isEnabled = true
                binding.btnGerarCodigo.isEnabled = true
            }
        }
    }

    private fun mostrarCodigoRecuperacao(codigo: String) {
        val espacamento = (24 * resources.displayMetrics.density).toInt()

        val textoCodigo = TextView(this).apply {
            text = "Guarde este código em um local seguro:\n\n" +
                    "$codigo\n\n" +
                    "Ele será exibido somente agora. " +
                    "Quem possuir este código poderá redefinir sua senha."

            textSize = 18f
            setPadding(espacamento, espacamento, espacamento, espacamento)
            setTextIsSelectable(true)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Seu código de recuperação")
            .setView(textoCodigo)
            .setPositiveButton("Guardei o código", null)
            .setCancelable(false)
            .show()
    }
}