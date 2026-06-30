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

            // Validação simples para não quebrar o app
            if (strPeso.isEmpty() || strAltura.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Criamos a Intent para mudar da MainActivity para a ResultadoActivity
            Intent intent = new Intent(MainActivity.this, ResultadoActivity.class);

            // Criamos o Bundle (a nossa "mala" de dados)
            Bundle dados = new Bundle();
            dados.putString("pesoDigitado", strPeso);
            dados.putString("alturaDigitada", strAltura);

            // Colocamos a mala dentro da Intent e iniciamos a nova tela
            intent.putExtras(dados);
            startActivity(intent);
        });
    }
}