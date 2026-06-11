package com.example.sorteio;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Declaração dos componentes
    private EditText editMinimo, editMaximo;
    private Button btnSortear;
    private TextView txtResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ligando os componentes do XML ao Java
        editMinimo = findViewById(R.id.editMinimo);
        editMaximo = findViewById(R.id.editMaximo);
        btnSortear = findViewById(R.id.btnSortear);
        txtResultado = findViewById(R.id.txtResultado);

        // Ação do Botão
        btnSortear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortear();
            }
        });
    }

    private void sortear() {
        // 1. Pegar os valores digitados
        String valorMin = editMinimo.getText().toString();
        String valorMax = editMaximo.getText().toString();

        // 2. Validação: verificar se os campos estão vazios
        if (valorMin.isEmpty() || valorMax.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha os dois campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Converter para números inteiros
        int min = Integer.parseInt(valorMin);
        int max = Integer.parseInt(valorMax);

        // 3. Validação: o mínimo não pode ser maior que o máximo
        if (min >= max) {
            Toast.makeText(this, "O valor mínimo deve ser menor que o máximo!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Lógica do Sorteio
        Random random = new Random();
        // A fórmula (max - min + 1) + min garante que o número esteja no intervalo
        int numeroSorteado = random.nextInt((max - min) + 1) + min;

        // 5. Exibir o resultado
        txtResultado.setText("Número sorteado: " + numeroSorteado);
    }
}