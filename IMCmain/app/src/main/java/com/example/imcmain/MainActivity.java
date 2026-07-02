package com.example.imcmain;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editPeso, editAltura;
    private Button btnCalcular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editPeso = findViewById(R.id.editPeso);
        editAltura = findViewById(R.id.editAltura);
        btnCalcular = findViewById(R.id.btnCalcular);

        btnCalcular.setOnClickListener(v -> {
            String strPeso = editPeso.getText().toString();
            String strAltura = editAltura.getText().toString();

            // Evita que o app quebre se clicar sem digitar nada
            if (strPeso.isEmpty() || strAltura.isEmpty()) {
                Toast.makeText(this, "Preencha o peso e a altura!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Intenção de ir para a outra tela
            Intent intent = new Intent(MainActivity.this, ResultadoActivity.class);

            // Cria a malinha (Bundle) e coloca os textos dentro com chaves identificadoras
            Bundle pacoteDados = new Bundle();
            pacoteDados.putString("chavePeso", strPeso);
            pacoteDados.putString("chaveAltura", strAltura);

            // Amarra a mala na intent e inicia a próxima activity
            intent.putExtras(pacoteDados);
            startActivity(intent);
        });
    }
}