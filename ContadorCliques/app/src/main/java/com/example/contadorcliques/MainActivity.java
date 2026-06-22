package com.example.contadorcliques;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
//1. armazena a quantidade de cliques
    private int contador = 0;
    // visuais declarados no xml
    private TextView textTitulo;
    private TextView textContador;
    private Button buttonClique;
    private Button buttonZerar;
//2. oncreate quando inicia a rodar (TELA INICIAL)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //vincula essa classe ao arquivo de layout do xml
        setContentView(R.layout.activity_main);
// 3. java busca no xml os elementos aqui embaixo
        textTitulo = findViewById(R.id.textTitulo);
        textContador = findViewById(R.id.textContador);
        buttonClique = findViewById(R.id.buttonClique);
        buttonZerar = findViewById(R.id.buttonZerar);
//4. titulo estatico
        textTitulo.setText("Contador de Cliques");
        //chama função do contador
        atualizarContador();
//5.  clicou contou, clique da o famoso ++ e atualiza na tela
        buttonClique.setOnClickListener(view -> {
            contador++;
            atualizarContador();
        });
//6. botao pra reiniciar o contador
        buttonZerar.setOnClickListener(view -> {
            contador = 0;
            atualizarContador();
        });
    }
//7. quando muda o numero a função joga no textview pra evitar repetiçaõ
    private void atualizarContador() {
        textContador.setText("Cliques: " + contador);
    }
}