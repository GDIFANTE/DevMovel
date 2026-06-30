package com.example.paint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class QuadradoPintura extends View {

    private Paint pincel;
    private Path caminhoLivre;

    // Variáveis para guardar as coordenadas onde o usuário tocou
    private float xInicial, yInicial, xAtual, yAtual;

    // Modos de desenho: 1 = Livre, 2 = Retângulo, 3 = Círculo
    private int formaAtual = 1;

    public QuadradoPintura(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializarPincel();
    }

    private void inicializarPincel() {
        pincel = new Paint();
        pincel.setColor(Color.BLACK); // Cor inicial
        pincel.setStyle(Paint.Style.STROKE); // Apenas contorno
        pincel.setStrokeWidth(10f); // Espessura do traço
        pincel.setAntiAlias(true); // Deixa as bordas lisas

        caminhoLivre = new Path();
    }

    // Métodos públicos para a MainActivity alterar o estado do pincel
    public void setMudarCor(int novaCor) {
        pincel.setColor(novaCor);
    }

    public void setForma(int novaForma) {
        this.formaAtual = novaForma;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (formaAtual == 1) {
            // Desenha a linha livre contínua
            canvas.drawPath(caminhoLivre, pincel);
        } else if (formaAtual == 2) {
            // Desenha um retângulo baseado no clique inicial e arrasto atual
            canvas.drawRect(xInicial, yInicial, xAtual, yAtual, pincel);
        } else if (formaAtual == 3) {
            // Calcula o raio baseado na distância arrastada
            float raio = (float) Math.hypot(xAtual - xInicial, yAtual - yInicial);
            canvas.drawCircle(xInicial, yInicial, raio, pincel);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        xAtual = event.getX();
        yAtual = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Quando encosta o dedo na tela
                xInicial = xAtual;
                yInicial = yAtual;
                if (formaAtual == 1) {
                    caminhoLivre.moveTo(xAtual, yAtual);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Quando arrasta o dedo
                if (formaAtual == 1) {
                    caminhoLivre.lineTo(xAtual, yAtual);
                }
                break;
        }

        invalidate(); // Força o Android a redesenhar a tela (chama o onDraw de novo)
        return true;
    }
}