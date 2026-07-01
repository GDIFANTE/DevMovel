package com.example.blocnotas;

public class Nota {
    private Integer idNota;
    private String titulo;
    private String txt;

    // Construtor para carregar dados do banco (com ID)
    public Nota(Integer idNota, String titulo, String txt) {
        this.idNota = idNota;
        this.titulo = titulo;
        this.txt = txt;
    }

    // Construtor para criar uma nova nota (sem ID ainda)
    public Nota(String titulo, String txt) {
        this.titulo = titulo;
        this.txt = txt;
    }

    public Integer getIdNota() { return idNota; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTxt() { return txt; }
    public void setTxt(String txt) { this.txt = txt; }

    // Facilita a exibição do título na ListView do Android
    @Override
    public String toString() {
        return this.titulo;
    }
}
