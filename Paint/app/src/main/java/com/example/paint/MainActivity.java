package com.example.paint;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private QuadradoPintura areaDesenho;
    private Button btnLivre, btnRetangulo, btnCirculo;
    private View viewPreto, viewVermelho;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        areaDesenho = findViewById(R.id.areaDesenho);
        btnLivre = findViewById(R.id.btnLivre);
        btnRetangulo = findViewById(R.id.btnRetangulo);
        btnCirculo = findViewById(R.id.btnCirculo);
        viewPreto = findViewById(R.id.viewPreto);
        viewVermelho = findViewById(R.id.viewVermelho);

        // Configuração dos Botões de Formas
        btnLivre.setOnClickListener(v -> areaDesenho.setForma(1));
        btnRetangulo.setOnClickListener(v -> areaDesenho.setForma(2));
        btnCirculo.setOnClickListener(v -> areaDesenho.setForma(3));

        // Configuração dos Cliques nas Cores
        viewPreto.setOnClickListener(v -> areaDesenho.setMudarCor(Color.BLACK));
        viewVermelho.setOnClickListener(v -> areaDesenho.setMudarCor(Color.RED));
    }
}