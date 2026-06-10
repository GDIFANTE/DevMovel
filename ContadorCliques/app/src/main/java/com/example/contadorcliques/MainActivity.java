package com.example.contadorcliques;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private int contador = 0;
    private TextView textTitulo;
    private TextView textContador;
    private Button buttonClique;
    private Button buttonZerar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textTitulo = findViewById(R.id.textTitulo);
        textContador = findViewById(R.id.textContador);
        buttonClique = findViewById(R.id.buttonClique);
        buttonZerar = findViewById(R.id.buttonZerar);

        textTitulo.setText("Contador de Cliques");
        atualizarContador();

        buttonClique.setOnClickListener(view -> {
            contador++;
            atualizarContador();
        });

        buttonZerar.setOnClickListener(view -> {
            contador = 0;
            atualizarContador();
        });
    }

    private void atualizarContador() {
        textContador.setText("Cliques: " + contador);
    }
}