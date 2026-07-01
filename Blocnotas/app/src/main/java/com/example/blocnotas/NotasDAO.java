package com.example.blocnotas;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class NotasDAO extends SQLiteOpenHelper {

    private static final String NOME_BANCO = "NotasDB";
    private static final int VERSAO = 1;

    public NotasDAO(Context context) {
        super(context, NOME_BANCO, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Cria a tabela exatamente como planejado na modelagem do PDF
        String sql = "CREATE TABLE notas (idNota INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT, txt TEXT);";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notas");
        onCreate(db);
    }

    // Cria/Insere uma nova nota no banco
    public void insereNota(Nota nota) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("titulo", nota.getTitulo());
        valores.put("txt", nota.getTxt());
        db.insert("notas", null, valores);
        db.close();
    }

    // Busca e monta a lista de todas as notas salvas
    public ArrayList<Nota> getListaNotas() {
        ArrayList<Nota> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notas", null);

        if (cursor.moveToFirst()) {
            do {
                Nota nota = new Nota(
                        cursor.getInt(0),  // idNota
                        cursor.getString(1), // titulo
                        cursor.getString(2)  // txt
                );
                lista.add(nota);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }
}
