package br.com.ergoperitus.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Usuario::class],
    version = 1,
    exportSchema = false
)
abstract class ErgoPeritusDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var instancia: ErgoPeritusDatabase? = null

        /**
         * Um banco Room é relativamente pesado. Por isso o aplicativo cria
         * uma única instância e a reaproveita em todas as telas.
         */
        fun obter(context: Context): ErgoPeritusDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    ErgoPeritusDatabase::class.java,
                    "ergo_peritus.db"
                ).build().also { bancoCriado ->
                    instancia = bancoCriado
                }
            }
        }
    }
}
