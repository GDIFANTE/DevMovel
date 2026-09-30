package br.com.ergoperitus.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Usuario::class,
        Evento::class,
        FuncaoEvento::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ErgoPeritusDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    abstract fun eventoDao(): EventoDao

    companion object {

        private val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE usuarios ADD COLUMN recuperacao_hash TEXT"
                )

                db.execSQL(
                    "ALTER TABLE usuarios ADD COLUMN recuperacao_salt TEXT"
                )
            }
        }

        private val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS eventos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        usuario_id INTEGER NOT NULL,
                        processo TEXT NOT NULL,
                        vara TEXT NOT NULL,
                        reclamante TEXT NOT NULL,
                        reclamada TEXT NOT NULL,
                        data TEXT NOT NULL,
                        criado_em INTEGER NOT NULL,
                        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_eventos_usuario_id
                    ON eventos (usuario_id)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS funcoes_evento (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        evento_id INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY (evento_id) REFERENCES eventos (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_funcoes_evento_evento_id
                    ON funcoes_evento (evento_id)
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instancia: ErgoPeritusDatabase? = null

        fun obter(context: Context): ErgoPeritusDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    ErgoPeritusDatabase::class.java,
                    "ergo_peritus.db"
                )
                    .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3)
                    .build()
                    .also { bancoCriado ->
                        instancia = bancoCriado
                    }
            }
        }
    }
}