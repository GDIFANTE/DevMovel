package com.example.blocnotas;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText editTituloNota, editTextoNota;
    private Button btnSalvarNota;
    private ListView listViewNotas;

    private NotaController controller;
    private ArrayAdapter<Nota> adapter;
    private ArrayList<Nota> listaDeNotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTituloNota = findViewById(R.id.editTituloNota);
        editTextoNota = findViewById(R.id.editTextoNota);
        btnSalvarNota = findViewById(R.id.btnSalvarNota);
        listViewNotas = findViewById(R.id.listViewNotas);

        // Inicializamos o Controller passando o contexto da tela
        controller = new NotaController(this);

        // Carrega a lista inicial vinda do Banco de Dados
        atualizarListaNaTela();

        // Ação de Salvar Nota ao clicar no botão
        btnSalvarNota.setOnClickListener(v -> {
            String titulo = editTituloNota.getText().toString();
            String texto = editTextoNota.getText().toString();

            if (titulo.isEmpty() || texto.isEmpty()) {
                Toast.makeText(this, "Preencha o título e o texto!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cria o objeto modelo da nota
            Nota novaNota = new Nota(titulo, texto);

            // Envia para o controlador salvar no SQLite
            controller.cadastrarNovaNota(novaNota);

            // Limpa os campos da tela para a próxima digitação
            editTituloNota.setText("");
            editTextoNota.setText("");

            // Atualiza a ListView com os novos dados gravados
            atualizarListaNaTela();
            Toast.makeText(this, "Nota salva com sucesso!", Toast.LENGTH_SHORT).show();
        });
    }

    private void atualizarListaNaTela() {
        // Busca a lista atualizada do banco
        listaDeNotas = controller.getListaNotas();

        // O ArrayAdapter mapeia a nossa lista e diz como o Android deve renderizá-la na tela
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaDeNotas);
        listViewNotas.setAdapter(adapter);
    }
}