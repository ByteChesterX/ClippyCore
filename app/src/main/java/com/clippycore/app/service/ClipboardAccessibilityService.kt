package com.clippycore.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.clippycore.app.data.database.AppDatabase
import com.clippycore.app.data.database.ClipboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ClipboardAccessibilityService - AccessibilityService kullanarak arka planda kopyalanan metinleri dinler
 * 
 * Bu servis, kullanıcının herhangi bir uygulamada kopyalama işlemi yaptığında
 * içeriği yakalar ve Room veritabanına kaydeder.
 * 
 * GEREKLİ İZİNLER (AndroidManifest.xml):
 * - android.permission.FOREGROUND_SERVICE
 * - android.permission.BIND_ACCESSIBILITY_SERVICE
 * 
 * KULLANIM:
 * 1. AndroidManifest.xml'e servis tanımı ekleyin
 * 2. Kullanıcıdan Accessibility iznini isteyin
 * 3. Servisi başlatın
 */
class ClipboardAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ClipboardAccService"
        
        /**
         * Servisin çalışıp çalışmadığını kontrol etmek için
         */
        var isRunning: Boolean = false
            private set
        
        /**
         * Singleton instance (gerektiğinde erişim için)
         */
        var instance: ClipboardAccessibilityService? = null
            private set

        /**
         * Son kopyalanan içeriği takip et (tekrarları önlemek için)
         */
        private var lastCopiedText: String? = null
        private var lastCopyTime: Long = 0L
        
        /**
         * Debounce süresi (ms) - Aynı içeriğin tekrar kaydedilmesini önler
         */
        private const val DEBOUNCE_TIME_MS = 1000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    private var clipboardManager: ClipboardManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        
        // ClipboardManager'ı başlat
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        // Service konfigürasyonu
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        Log.d(TAG, "Accessibility Service connected and ready to monitor clipboard")
        
        // Periyodik clipboard kontrolünü başlat
        startClipboardMonitoring()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // Metin seçimi değiştiğinde kontrol et
                handleTextSelection(event)
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Pencere değiştiğinde kontrol et
                checkClipboard()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Accessibility Service destroyed")
    }

    /**
     * Periyodik olarak clipboard'ı kontrol eden döngüyü başlat
     */
    private fun startClipboardMonitoring() {
        serviceScope.launch {
            while (isRunning) {
                checkClipboard()
                delay(500) // Her 500ms'de bir kontrol et
            }
        }
    }

    /**
     * Clipboard içeriğini kontrol et ve yeni içerik varsa kaydet
     */
    private fun checkClipboard() {
        try {
            val clipData = clipboardManager?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val currentItem = clipData.getItemAt(0)
                val text = currentItem.text?.toString()
                
                if (!text.isNullOrEmpty()) {
                    // Debounce kontrolü
                    val currentTime = System.currentTimeMillis()
                    if (text != lastCopiedText || (currentTime - lastCopyTime) > DEBOUNCE_TIME_MS) {
                        lastCopiedText = text
                        lastCopyTime = currentTime
                        
                        // Veritabanına kaydet
                        saveToDatabase(text)
                        
                        Log.d(TAG, "New clipboard content detected: ${text.take(50)}...")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking clipboard", e)
        }
    }

    /**
     * Metin seçimi olayını işle
     */
    private fun handleTextSelection(event: AccessibilityEvent) {
        val source = event.source ?: return
        
        try {
            // Seçili metni al
            val selectedText = source.text?.toString()
            if (!selectedText.isNullOrEmpty()) {
                // Seçilen metni de kaydedebiliriz (opsiyonel)
                // saveToDatabase(selectedText)
            }
        } finally {
            source.recycle()
        }
    }

    /**
     * Clipboard içeriğini veritabanına kaydet
     */
    private fun saveToDatabase(content: String) {
        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).clipboardDao()
                
                // İçerik tipini belirle
                val itemType = determineItemType(content)
                
                val clipboardItem = ClipboardItem(
                    content = content,
                    itemType = itemType,
                    timestamp = System.currentTimeMillis(),
                    characterCount = content.length
                )
                
                dao.insert(clipboardItem)
                
                // Eski kayıtları temizle (performans için)
                dao.trimToMaxCount(1000)
                
                Log.d(TAG, "Saved to database: ${content.take(30)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to database", e)
            }
        }
    }

    /**
     * İçeriğin tipini belirle
     */
    private fun determineItemType(content: String): ClipboardItem.ItemType {
        return when {
            // JSON tespiti
            content.trim().startsWith("{") || content.trim().startsWith("[") -> {
                ClipboardItem.ItemType.JSON
            }
            // Email tespiti
            content.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> {
                ClipboardItem.ItemType.EMAIL
            }
            // Telefon numarası tespiti (Türkiye formatı dahil)
            content.matches(Regex("^\\+?[0-9\\s()-]{8,20}$")) -> {
                ClipboardItem.ItemType.PHONE
            }
            // URL tespiti
            content.matches(Regex("^https?://[\\w.-]+.*$")) -> {
                ClipboardItem.ItemType.URL
            }
            // XML tespiti
            content.trim().startsWith("<") && content.trim().endsWith(">") -> {
                ClipboardItem.ItemType.XML
            }
            else -> {
                ClipboardItem.ItemType.PLAIN_TEXT
            }
        }
    }

    /**
     * Manuel olarak clipboard içeriğini getir (gerektiğinde)
     */
    fun getCurrentClipboardContent(): String? {
        return try {
            clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current clipboard content", e)
            null
        }
    }
}
