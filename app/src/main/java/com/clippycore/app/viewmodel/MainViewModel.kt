package com.clippycore.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.clippycore.app.data.database.AppDatabase
import com.clippycore.app.data.database.ClipboardDao
import com.clippycore.app.data.database.ClipboardItem
import com.clippycore.app.util.TextTransformer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * MainViewModel - Ana ekran için ViewModel
 * 
 * Sorumluluklar:
 * - UI state yönetimi
 * - Clipboard verilerinin yönetimi (CRUD)
 * - Google Play Billing entegrasyonu
 * - Arama ve filtreleme
 * - Dönüştürme işlemleri
 */
class MainViewModel(application: Application) : AndroidViewModel(application), PurchasesUpdatedListener {

    // Database DAO
    private val clipboardDao: ClipboardDao = AppDatabase.getDatabase(application).clipboardDao()
    
    // Uygulama context
    private val appContext = application.applicationContext

    // UI State - Clipboard öğeleri
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Arama sorgusu
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Billing Client
    private val _billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    // Premium durum
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    // Ürün ID'leri (Google Play Console'dan alınmalı)
    companion object {
        const val PREMIUM_PRODUCT_ID = "clippy_premium_lifetime"
        private const val TAG = "MainViewModel"
    }

    init {
        // Başlangıçta verileri yükle
        loadClipboardItems()
        
        // Premium durumunu kontrol et
        checkPremiumStatus()
        
        // Billing client'ı başlat
        startBillingConnection()
    }

    /**
     * Clipboard öğelerini yükle ve Flow ile dinle
     */
    private fun loadClipboardItems() {
        viewModelScope.launch {
            clipboardDao.getAllItems().collect { items ->
                _uiState.update { currentState ->
                    currentState.copy(
                        clipboardItems = items,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Arama sorgusunu güncelle
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        viewModelScope.launch {
            if (query.isNotBlank()) {
                clipboardDao.searchItems(query).collect { filteredItems ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            clipboardItems = filteredItems
                        )
                    }
                }
            } else {
                loadClipboardItems()
            }
        }
    }

    /**
     * Favori durumunu değiştir
     */
    fun toggleFavorite(item: ClipboardItem) {
        viewModelScope.launch {
            clipboardDao.updateFavoriteStatus(item.id, !item.isFavorite)
        }
    }

    /**
     * Öğeyi sil
     */
    fun deleteItem(item: ClipboardItem) {
        viewModelScope.launch {
            clipboardDao.delete(item)
        }
    }

    /**
     * Tüm geçmişi temizle
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            clipboardDao.deleteAll()
        }
    }

    /**
     * Eski kayıtları temizle (performans optimizasyonu)
     */
    fun trimOldItems(maxCount: Int = 1000) {
        viewModelScope.launch {
            clipboardDao.trimToMaxCount(maxCount)
        }
    }

    /**
     * İçeriği dönüştür (JSON format, maskeleme vb.)
     */
    fun transformContent(item: ClipboardItem, transformType: TransformType) {
        viewModelScope.launch {
            val transformedContent = when (transformType) {
                TransformType.FORMAT_JSON -> {
                    TextTransformer.formatJson(item.content)
                }
                TransformType.MASK_SENSITIVE -> {
                    TextTransformer.autoMaskSensitiveData(item.content)
                }
                TransformType.MASK_EMAIL -> {
                    TextTransformer.maskEmail(item.content)
                }
                TransformType.MASK_PHONE -> {
                    TextTransformer.maskPhone(item.content)
                }
                TransformType.FORMAT_DATE -> {
                    TextTransformer.formatDate(item.content)
                }
            }

            // Veritabanına kaydet
            clipboardDao.updateTransformedContent(item.id, transformedContent)
            
            // Kullanıcıya bilgi ver (event olarak gönderilebilir)
            _uiState.update { currentState ->
                currentState.copy(
                    message = "Dönüştürme başarılı!"
                )
            }
        }
    }

    // ==================== BILLING İŞLEMLERİ ====================

    /**
     * Billing bağlantısını başlat
     */
    private fun startBillingConnection() {
        _billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    // Bağlantı başarılı, satın alma durumunu kontrol et
                    checkPremiumStatus()
                } else {
                    // Bağlantı hatası
                    _uiState.update { currentState ->
                        currentState.copy(
                            billingError = "Billing bağlantı hatası: ${billingResult.debugMessage}"
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Bağlantı kesildi, yeniden bağlanmayı dene
                _billingClient.startConnection(this)
            }
        })
    }

    /**
     * Premium durumunu kontrol et
     */
    private fun checkPremiumStatus() {
        viewModelScope.launch {
            try {
                _billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                ) { _, purchasesList ->
                    val isUserPremium = purchasesList?.any { purchase ->
                        purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    } ?: false
                    
                    _isPremium.value = isUserPremium
                    
                    // UI state'i güncelle
                    _uiState.update { currentState ->
                        currentState.copy(
                            isPremiumUser = isUserPremium
                        )
                    }
                }
            } catch (e: Exception) {
                // Hata durumunda premium değil varsay
                _isPremium.value = false
            }
        }
    }

    /**
     * Premium ürün satın almasını başlat
     */
    fun initiatePremiumPurchase(activity: androidx.activity.ComponentActivity) {
        viewModelScope.launch {
            try {
                val productDetails = getProductDetails(PREMIUM_PRODUCT_ID)
                if (productDetails == null) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            billingError = "Ürün bilgileri bulunamadı"
                        )
                    }
                    return@launch
                }
                
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                _billingClient.launchBillingFlow(activity, flowParams)
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        billingError = "Satın alma başlatma hatası: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Ürün detaylarını getir (suspend function)
     */
    private suspend fun getProductDetails(productId: String): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            _billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    val details = productDetailsList.firstOrNull { it.productId == productId }
                    continuation.resume(details) {}
                } else {
                    continuation.resume(null) {}
                }
            }
        }
    }

    /**
     * Satın alma güncellemelerini dinle
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            // Kullanıcı satın almayı iptal etti
            _uiState.update { currentState ->
                currentState.copy(
                    message = "Satın alma iptal edildi"
                )
            }
        } else {
            // Diğer hatalar
            _uiState.update { currentState ->
                currentState.copy(
                    billingError = "Satın alma hatası: ${billingResult.debugMessage}"
                )
            }
        }
    }

    /**
     * Satın alma işlemini işle
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(PREMIUM_PRODUCT_ID)) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Satın alma başarılı
                _isPremium.value = true
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isPremiumUser = true,
                        message = "Premium özellikler aktif edildi!"
                    )
                }

                // Satın almayı onayla
                acknowledgePurchase(purchase.purchaseToken)
            }
        }
    }

    /**
     * Satın almayı onayla (acknowledge)
     */
    private fun acknowledgePurchase(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        _billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                // Onaylama başarılı
                Log.d(TAG, "Purchase acknowledged successfully")
            }
        }
    }

    /**
     * ViewModel temizlendiğinde kaynakları serbest bırak
     */
    override fun onCleared() {
        super.onCleared()
        if (_billingClient.isReady) {
            _billingClient.endConnection()
        }
    }

    /**
     * Mesajı temizle
     */
    fun clearMessage() {
        _uiState.update { currentState ->
            currentState.copy(message = null)
        }
    }

    /**
     * Billing hatasını temizle
     */
    fun clearBillingError() {
        _uiState.update { currentState ->
            currentState.copy(billingError = null)
        }
    }
}

/**
 * UI State data class
 */
data class MainUiState(
    val clipboardItems: List<ClipboardItem> = emptyList(),
    val isLoading: Boolean = true,
    val isPremiumUser: Boolean = false,
    val message: String? = null,
    val billingError: String? = null
)

/**
 * Dönüştürme tipleri enum - TextTransformer.kt'den ayrı olarak burada tanımlanır
 */
enum class TransformType {
    FORMAT_JSON,
    MASK_SENSITIVE,
    MASK_EMAIL,
    MASK_PHONE,
    FORMAT_DATE
}
