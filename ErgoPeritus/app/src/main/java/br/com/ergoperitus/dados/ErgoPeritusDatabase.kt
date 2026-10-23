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
        FuncaoEvento::class,
        Atividade::class,
        ChecklistOcra::class,
        ForcaOcra::class,
        PosturaOcra::class,
        ComplementaresOcra::class
    ],
    version = 8,
    exportSchema = false
)
abstract class ErgoPeritusDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    abstract fun eventoDao(): EventoDao

    abstract fun atividadeDao(): AtividadeDao

    abstract fun checklistOcraDao(): ChecklistOcraDao

    abstract fun forcaOcraDao(): ForcaOcraDao

    abstract fun posturaOcraDao(): PosturaOcraDao

    abstract fun complementaresOcraDao(): ComplementaresOcraDao

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

        private val MIGRACAO_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS atividades (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        funcao_id INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        descricao TEXT NOT NULL,
                        duracao_diaria_minutos INTEGER,
                        criado_em INTEGER NOT NULL,
                        atualizado_em INTEGER NOT NULL,
                        FOREIGN KEY (funcao_id) REFERENCES funcoes_evento (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_atividades_funcao_id
                    ON atividades (funcao_id)
                    """.trimIndent()
                )
            }
        }

        private val MIGRACAO_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS checklists_ocra (
                        atividade_id INTEGER NOT NULL,
                        recuperacao INTEGER,
                        jornada_minutos INTEGER,
                        distribuicao_pausas TEXT NOT NULL,
                        contagem_direita INTEGER NOT NULL,
                        tempo_direita_ms INTEGER NOT NULL,
                        medicao_direita_concluida INTEGER NOT NULL,
                        contagem_esquerda INTEGER NOT NULL,
                        tempo_esquerda_ms INTEGER NOT NULL,
                        medicao_esquerda_concluida INTEGER NOT NULL,
                        frequencia_dinamica_direita REAL,
                        frequencia_dinamica_esquerda REAL,
                        frequencia_estatica_direita REAL,
                        frequencia_estatica_esquerda REAL,
                        atualizado_em INTEGER NOT NULL,
                        PRIMARY KEY (atividade_id),
                        FOREIGN KEY (atividade_id) REFERENCES atividades (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRACAO_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS forca_ocra (
                        atividade_id INTEGER NOT NULL,
                        lado TEXT NOT NULL,
                        ha_forca_repetida INTEGER,
                        borg_moderada INTEGER,
                        tempo_moderada TEXT,
                        borg_forte INTEGER,
                        tempo_forte TEXT,
                        borg_quase_maxima INTEGER,
                        tempo_quase_maxima TEXT,
                        observacoes TEXT NOT NULL,
                        atualizado_em INTEGER NOT NULL,
                        PRIMARY KEY (atividade_id, lado),
                        FOREIGN KEY (atividade_id) REFERENCES atividades (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRACAO_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS postura_ocra (
                        atividade_id INTEGER NOT NULL,
                        lado TEXT NOT NULL,
                        ombro INTEGER,
                        maos_acima_cabeca INTEGER,
                        cotovelo INTEGER,
                        punho INTEGER,
                        mao_dedos INTEGER,
                        preensoes INTEGER,
                        estereotipia REAL,
                        observacoes TEXT NOT NULL,
                        atualizado_em INTEGER NOT NULL,
                        PRIMARY KEY (atividade_id, lado),
                        FOREIGN KEY (atividade_id) REFERENCES atividades (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRACAO_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS complementares_ocra (
                        atividade_id INTEGER NOT NULL,
                        lado TEXT NOT NULL,
                        fatores INTEGER,
                        pontuacao_fisica INTEGER,
                        ritmo_maquina INTEGER,
                        observacoes TEXT NOT NULL,
                        atualizado_em INTEGER NOT NULL,
                        PRIMARY KEY (atividade_id, lado),
                        FOREIGN KEY (atividade_id) REFERENCES atividades (id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
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
                    .addMigrations(
                        MIGRACAO_1_2,
                        MIGRACAO_2_3,
                        MIGRACAO_3_4,
                        MIGRACAO_4_5,
                        MIGRACAO_5_6,
                        MIGRACAO_6_7,
                        MIGRACAO_7_8
                    )
                    .build()
                    .also { bancoCriado ->
                        instancia = bancoCriado
                    }
            }
        }
    }
}