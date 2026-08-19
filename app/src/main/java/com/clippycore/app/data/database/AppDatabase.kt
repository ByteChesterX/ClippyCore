package com.clippycore.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AppDatabase - Room Database ana sınıfı
 * 
 * Veritabanı yapılandırması, tablo tanımları ve singleton instance yönetimi
 */
@Database(
    entities = [ClipboardItem::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * ClipboardDao için abstract getter
     */
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Singleton pattern ile veritabanı instance'ını getir
         * 
         * @param context Application context
         * @return AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clipboard_database"
                )
                    .fallbackToDestructiveMigration() // Şema değişikliklerinde verileri silerek migrate et
                    .addCallback(object : Callback() {
                        override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
                            super.onCreate(db)
                            // Veritabanı ilk oluşturulduğunda çalışacak kodlar
                            // Gerekirse başlangıç verileri eklenebilir
                        }

                        override fun onOpen(db: android.database.sqlite.SQLiteDatabase) {
                            super.onOpen(db)
                            // Veritabanı her açıldığında çalışacak kodlar
                            // Örneğin: Foreign key'leri aktif et
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Veritabanı instance'ını sıfırla (test amaçlı)
         */
        fun resetDatabase() {
            INSTANCE = null
        }
    }
}
