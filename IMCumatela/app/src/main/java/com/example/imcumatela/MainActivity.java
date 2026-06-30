package com.example.imcumatela;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.imcumatela.R;

public class MainActivity extends AppCompatActivity {

    private EditText editPeso, editAltura;
    private TextView textResultadoIMC, textClassificacao;
    private ImageView imageResultado;
    private Button btnCalcular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Vinculando componentes
        editPeso = findViewById(R.id.editPeso);
        editAltura = findViewById(R.id.editAltura);
        textResultadoIMC = findViewById(R.id.textResultadoIMC);
        textClassificacao = findViewById(R.id.textClassificacao);
        imageResultado = findViewById(R.id.imageResultado);
        btnCalcular = findViewById(R.id.btnCalcular);

        // 2. Evento de clique
        btnCalcular.setOnClickListener(v -> {
            calcularIMC();
        });
    }

    private void calcularIMC() {
        // Recuperar valores digitados
        String strPeso = editPeso.getText().toString();
        String strAltura = editAltura.getText().toString();

        // Validação simples: verificar se os campos estão vazios
        if (strPeso.isEmpty() || strAltura.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Converter texto para números (Double permite casas decimais)
        double peso = Double.parseDouble(strPeso);
        double altura = Double.parseDouble(strAltura);

        // A FÓRMULA: Peso dividido por (Altura vezes Altura)
        double imc = peso / (altura * altura);

        // Formatar o resultado para aparecer com 2 casas decimais apenas
        textResultadoIMC.setText(String.format("Resultado: %.2f", imc));

        // Lógica de classificação e troca de imagem
        if (imc < 18.5) {
            textClassificacao.setText("Abaixo do peso");
            imageResultado.setImageResource(R.drawable.abaixopeso);
        } else if (imc >= 18.5 && imc <= 24.9) {
            textClassificacao.setText("Peso normal");
            imageResultado.setImageResource(R.drawable.normal);
        } else if (imc >= 25 && imc <= 29.9) {
            textClassificacao.setText("Sobrepeso");
            imageResultado.setImageResource(R.drawable.sobrepeso);
        } else if (imc >= 30 && imc <= 34.9) {
            textClassificacao.setText("Obesidade Grau 1");
            imageResultado.setImageResource(R.drawable.obesidade1);
        } else if (imc >= 35 && imc <= 39.9) {
            textClassificacao.setText("Obesidade Grau 2");
            imageResultado.setImageResource(R.drawable.obesidade2);
        } else {
            textClassificacao.setText("Obesidade Grau 3");
            imageResultado.setImageResource(R.drawable.obesidade3);
        }
    }
}