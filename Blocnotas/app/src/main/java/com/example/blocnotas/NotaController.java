package com.example.blocnotas;

import android.content.Context;
import java.util.ArrayList;

public class NotaController {

    private NotasDAO notasDAO;

    public NotaController(Context context) {
        this.notasDAO = new NotasDAO(context);
    }

    public void cadastrarNovaNota(Nota nota) {
        notasDAO.insereNota(nota);
    }

    public ArrayList<Nota> getListaNotas() {
        return notasDAO.getListaNotas();
    }
}
