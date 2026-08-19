package com.clippycore.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * ClipboardDao - Room Database için veri erişim katmanı
 * 
 * Tüm CRUD operasyonları ve özel sorgular bu arayüzde tanımlanır.
 */
@Dao
interface ClipboardDao {

    /**
     * Yeni bir clipboard öğesi ekle
     * @param item Eklenecek ClipboardItem
     * @return Eklenen öğenin ID'si
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    /**
     * Birden fazla clipboard öğesi ekle
     * @param items Eklenecek öğeler listesi
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ClipboardItem>)

    /**
     * ID'ye göre sil
     * @param id Silinecek öğenin ID'si
     */
    @Delete
    suspend fun delete(item: ClipboardItem)

    /**
     * ID'ye göre sil
     * @param id Silinecek öğenin ID'si
     */
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Tüm kayıtları sil
     */
    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    /**
     * Tüm clipboard öğelerini zamana göre azalan sırada getir (Flow)
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItem>>

    /**
     * Tüm clipboard öğelerini zamana göre azalan sırada getir (List)
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    suspend fun getAllItemsList(): List<ClipboardItem>

    /**
     * Belirli bir limit ile en son öğeleri getir
     * @param limit Kaç öğe getirileceği
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentItems(limit: Int = 50): List<ClipboardItem>

    /**
     * Flow ile en son öğeleri getir
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentItemsFlow(limit: Int = 50): Flow<List<ClipboardItem>>

    /**
     * ID'ye göre öğe getir
     */
    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getItemById(id: Long): ClipboardItem?

    /**
     * Favori öğeleri getir
     */
    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteItems(): Flow<List<ClipboardItem>>

    /**
     * İçeriğe göre arama yap
     * @param query Arama sorgusu
     */
    @Query("""
        SELECT * FROM clipboard_items 
        WHERE content LIKE '%' || :query || '%' 
           OR transformedContent LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchItems(query: String): Flow<List<ClipboardItem>>

    /**
     * İçerik türüne göre filtrele
     */
    @Query("SELECT * FROM clipboard_items WHERE itemType = :type ORDER BY timestamp DESC")
    fun getItemsByType(type: ClipboardItem.ItemType): Flow<List<ClipboardItem>>

    /**
     * Tarih aralığına göre öğe getir
     * @param startTime Başlangıç zamanı (timestamp)
     * @param endTime Bitiş zamanı (timestamp)
     */
    @Query("SELECT * FROM clipboard_items WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getItemsByDateRange(startTime: Long, endTime: Long): List<ClipboardItem>

    /**
     * Toplam kayıt sayısını getir
     */
    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getCount(): Int

    /**
     * Favori durumunu güncelle
     */
    @Query("UPDATE clipboard_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    /**
     * Dönüştürülmüş içeriği güncelle
     */
    @Query("UPDATE clipboard_items SET transformedContent = :transformedContent WHERE id = :id")
    suspend fun updateTransformedContent(id: Long, transformedContent: String?)

    /**
     * En eski kayıtları sil (sayıyı sınırla)
     * @param maxCount Maksimum tutulacak kayıt sayısı
     */
    @Query("""
        DELETE FROM clipboard_items 
        WHERE id NOT IN (
            SELECT id FROM clipboard_items 
            ORDER BY timestamp DESC 
            LIMIT :maxCount
        )
    """)
    suspend fun trimToMaxCount(maxCount: Int = 1000)

    /**
     * Belirli bir süreden eski kayıtları sil (gün cinsinden)
     * @param daysOld Kaç günden eski kayıtlar silinecek
     */
    @Query("""
        DELETE FROM clipboard_items 
        WHERE timestamp < (:currentTime - (:daysOld * 24 * 60 * 60 * 1000))
        AND isFavorite = 0
    """)
    suspend fun deleteOldItems(daysOld: Int, currentTime: Long = System.currentTimeMillis())
}
