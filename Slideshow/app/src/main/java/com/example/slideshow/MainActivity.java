package com.example.slideshow;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // 1. variáveis dops componentes de tela
    private ImageView imageViewSlide;
    private Button buttonAnterior;
    private Button buttonProximo;

    // 2. array com as imagens do drawable
    private int[] minhasImagens = {
            R.drawable.cachorro,
            R.drawable.gardem,
            R.drawable.happy,
            R.drawable.patinho,
            R.drawable.porquinho
    };

    // variavel pra iomagem que aparece primeiro na tela
    private int indiceAtual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 3. vinculo das variaveis do java com o xml
        imageViewSlide = findViewById(R.id.imageViewSlide);
        buttonAnterior = findViewById(R.id.buttonAnterior);
        buttonProximo = findViewById(R.id.buttonProximo);

        // 4. config do botao proximo
        buttonProximo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // anvança mais um na lista de imagens
                indiceAtual++;

                // no final volta pro zero
                if (indiceAtual >= minhasImagens.length) {
                    indiceAtual = 0;
                }

                // Atualiza a imagem na tela
                imageViewSlide.setImageResource(minhasImagens[indiceAtual]);
            }
        });

        // 5. config do botao anterior
        buttonAnterior.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Volta um índice na lista de imagens
                indiceAtual--;

                // Se estiver na primeira e clicar em voltar, vai para a última imagem
                if (indiceAtual < 0) {
                    indiceAtual = minhasImagens.length - 1;
                }

                // Atualiza a imagem na tela
                imageViewSlide.setImageResource(minhasImagens[indiceAtual]);
            }
        });
    }
}