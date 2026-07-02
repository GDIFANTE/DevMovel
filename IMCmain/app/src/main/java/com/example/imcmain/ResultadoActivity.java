package com.example.imcmain;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultadoActivity extends AppCompatActivity {

    private TextView textValorIMC, textVeredito;
    private ImageView imageVeredito;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        textValorIMC = findViewById(R.id.textValorIMC);
        textVeredito = findViewById(R.id.textVeredito);
        imageVeredito = findViewById(R.id.imageVeredito);

        // Resgata o pacote enviado pela MainActivity
        Bundle pacoteRecebido = getIntent().getExtras();

        if (pacoteRecebido != null) {
            // Extrai as Strings usando as mesmas chaves do envio
            String strPeso = pacoteRecebido.getString("chavePeso");
            String strAltura = pacoteRecebido.getString("chaveAltura");

            // Converte para tipo flutuante (Double) para fazer contas matemáticas
            double peso = Double.parseDouble(strPeso);
            double altura = Double.parseDouble(strAltura);

            // Realiza a fórmula matemática do IMC
            double imc = peso / (altura * altura);

            // Formata o número na interface para mostrar apenas duas casas decimais
            textValorIMC.setText(String.format("IMC: %.2f", imc));

            // Aplica as regras da tabela do professor
            if (imc < 18.5) {
                textVeredito.setText("Abaixo do peso");
                imageVeredito.setImageResource(R.drawable.abaixopeso);
            } else if (imc >= 18.5 && imc <= 24.9) {
                textVeredito.setText("Peso normal");
                imageVeredito.setImageResource(R.drawable.normal);
            } else if (imc >= 25 && imc <= 29.9) {
                textVeredito.setText("Sobrepeso");
                imageVeredito.setImageResource(R.drawable.sobrepeso);
            } else if (imc >= 30 && imc <= 34.9) {
                textVeredito.setText("Obesidade grau 1");
                imageVeredito.setImageResource(R.drawable.obesidade1);
            } else if (imc >= 35 && imc <= 39.9) {
                textVeredito.setText("Obesidade grau 2");
                imageVeredito.setImageResource(R.drawable.obesidade2);
            } else {
                textVeredito.setText("Obesidade grau 3");
                imageVeredito.setImageResource(R.drawable.obesidade3);
            }
        }
    }
}