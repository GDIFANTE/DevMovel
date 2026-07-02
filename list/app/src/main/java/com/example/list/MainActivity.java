package com.example.list;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ListView listViewExemplo;

    // Lista dinâmica para armazenar nossos dados (Strings)
    private ArrayList<String> listaContatos;

    // O Adaptador que vai converter os textos em elementos visuais de linha
    private ArrayAdapter<String> adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewExemplo = findViewById(R.id.listViewExemplo);

        // 1. Cria e alimenta a lista de dados brutos
        listaContatos = new ArrayList<>();
        listaContatos.add("Guilherme");
        listaContatos.add("Deise");
        listaContatos.add("Pedro Barbosa");
        listaContatos.add("Sabrina");
        listaContatos.add("Ana Maria");
        listaContatos.add("Carlos Eduardo");

        // 2. Inicializa o ArrayAdapter
        // Parâmetros: (Contexto, Layout padrão de linha do Android, Nossa lista de dados)
        adaptador = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                listaContatos
        );

        // 3. Vincula o adaptador à ListView da tela
        listViewExemplo.setAdapter(adaptador);

        // 4. Tratando o evento de clique em um item da lista
        listViewExemplo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Pega o texto correspondente à posição clicada
                String nomeSelecionado = listaContatos.get(position);

                // Exibe o balão de aviso com o nome
                Toast.makeText(MainActivity.this, "Clicou em: " + nomeSelecionado, Toast.LENGTH_SHORT).show();
            }
        });
    }
}