package com.clippycore.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ClipboardItem Entity - Room veritabanı için kopyalanan metinleri temsil eder.
 * 
 * @property id Benzersiz kimlik (otomatik artan)
 * @property content Kopyalanan metin içeriği
 * @property timestamp Kopyalanma zamanı (Unix timestamp)
 * @property itemType İçeriğin tipi (PLAIN_TEXT, JSON, EMAIL, PHONE vb.)
 * @property isFavorite Kullanıcı tarafından favorilenmiş mi?
 * @property transformedContent Dönüştürülmüş içerik (varsa)
 */
@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val content: String,
    
    val timestamp: Long = System.currentTimeMillis(),
    
    val itemType: ItemType = ItemType.PLAIN_TEXT,
    
    val isFavorite: Boolean = false,
    
    val transformedContent: String? = null,
    
    val sourcePackage: String? = null, // Hangi uygulamadan kopyalandı
    val characterCount: Int = content.length
) {
    /**
     * İçerik türlerini belirlemek için Enum
     */
    enum class ItemType {
        PLAIN_TEXT,
        JSON,
        XML,
        EMAIL,
        PHONE,
        URL,
        DATE,
        TIME,
        CURRENCY,
        ADDRESS,
        CODE_SNIPPET
    }
}
