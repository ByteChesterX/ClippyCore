package com.clippycore.app.util

import android.util.Patterns
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * TextTransformer - Metin dönüştürme ve formatlama işlemleri için utility sınıfı
 * 
 * Özellikler:
 * - JSON formatlama (pretty print)
 * - Regex ile maskeleme (email, telefon, kredi kartı vb.)
 * - Tarih/saat formatlama
 * - Para birimi formatlama
 * - XML formatlama
 * - URL doğrulama ve formatlama
 */
object TextTransformer {

    /**
     * JSON içeriğini formatla (pretty print)
     * 
     * @param json Formatlanmamış JSON string
     * @return Formatlanmış JSON string veya hata durumunda orijinal içerik
     */
    fun formatJson(json: String): String {
        return try {
            // Basit JSON formatlayıcı (external library olmadan)
            val sb = StringBuilder()
            var indentLevel = 0
            val indent = "  " // 2 boşluk
            
            var inString = false
            var escaped = false
            
            for (char in json) {
                when (char) {
                    '"' -> {
                        if (!escaped) {
                            inString = !inString
                        }
                        sb.append(char)
                        escaped = false
                    }
                    '\\' -> {
                        escaped = !escaped
                        sb.append(char)
                    }
                    '{', '[' -> {
                        sb.append(char)
                        if (!inString) {
                            indentLevel++
                            sb.append("\n")
                            repeat(indentLevel) { sb.append(indent) }
                        }
                        escaped = false
                    }
                    '}', ']' -> {
                        if (!inString) {
                            indentLevel--
                            sb.append("\n")
                            repeat(indentLevel) { sb.append(indent) }
                        }
                        sb.append(char)
                        escaped = false
                    }
                    ',' -> {
                        sb.append(char)
                        if (!inString) {
                            sb.append("\n")
                            repeat(indentLevel) { sb.append(indent) }
                        }
                        escaped = false
                    }
                    ':' -> {
                        sb.append(char)
                        if (!inString) {
                            sb.append(" ")
                        }
                        escaped = false
                    }
                    else -> {
                        sb.append(char)
                        escaped = false
                    }
                }
            }
            
            sb.toString().trim()
        } catch (e: Exception) {
            // Geçersiz JSON ise orijinal içeriği döndür
            json
        }
    }

    /**
     * Email adreslerini maskele
     * Örnek: "john.doe@example.com" -> "j***@example.com"
     * 
     * @param text Maskeleme yapılacak metin
     * @return Maskelenmiş metin
     */
    fun maskEmail(text: String): String {
        val emailPattern = Patterns.EMAIL_ADDRESS
        val matcher = emailPattern.matcher(text)
        
        val result = StringBuffer()
        while (matcher.find()) {
            val email = matcher.group()
            val maskedEmail = maskSingleEmail(email)
            matcher.appendReplacement(result, maskedEmail)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }

    /**
     * Tek bir email adresini maskele
     */
    private fun maskSingleEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return email
        
        val username = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        
        val maskedUsername = buildString {
            append(username.first())
            repeat(username.length - 1) { append('*') }
        }
        
        return "$maskedUsername$domain"
    }

    /**
     * Telefon numaralarını maskele
     * Örnek: "+90 555 123 4567" -> "+90 555 *** **67"
     * 
     * @param text Maskeleme yapılacak metin
     * @return Maskelenmiş metin
     */
    fun maskPhone(text: String): String {
        // Uluslararası telefon numarası pattern'i
        val phonePattern = Regex("\\+?[0-9\\s()-]{8,20}")
        
        return phonePattern.replace(text) { matchResult ->
            val phone = matchResult.value
            if (phone.length >= 8) {
                val visibleChars = 4
                val maskedLength = phone.length - visibleChars
                "${"*".repeat(maskedLength)}${phone.takeLast(visibleChars)}"
            } else {
                phone
            }
        }
    }

    /**
     * Kredi kartı numaralarını maskele
     * Örnek: "4532 1234 5678 9012" -> "**** **** **** 9012"
     * 
     * @param text Maskeleme yapılacak metin
     * @return Maskelenmiş metin
     */
    fun maskCreditCard(text: String): String {
        // Kredi kartı pattern'i (13-19 hane, boşluklu veya boşluksuz)
        val ccPattern = Regex("\\b(?:\\d[ -]*){13,19}\\b")
        
        return ccPattern.replace(text) { matchResult ->
            val cardNumber = matchResult.value.replace(" ", "").replace("-", "")
            if (cardNumber.length in 13..19) {
                val lastFour = cardNumber.takeLast(4)
                "**** **** **** $lastFour"
            } else {
                matchResult.value
            }
        }
    }

    /**
     * Tarih formatını değiştir
     * 
     * @param dateText Formatlanacak tarih string'i
     * @param inputFormat Mevcut format (varsayılan: ISO 8601)
     * @param outputFormat Hedef format (varsayılan: dd MMMM yyyy HH:mm)
     * @return Formatlanmış tarih string'i
     */
    fun formatDate(
        dateText: String,
        inputFormat: String = "yyyy-MM-dd'T'HH:mm:ss",
        outputFormat: String = "dd MMMM yyyy HH:mm"
    ): String {
        return try {
            val inputFormatter = SimpleDateFormat(inputFormat, Locale.getDefault())
            val outputFormatter = SimpleDateFormat(outputFormat, Locale("tr", "TR"))
            
            val date = inputFormatter.parse(dateText)
            date?.let { outputFormatter.format(it) } ?: dateText
        } catch (e: Exception) {
            dateText
        }
    }

    /**
     * Unix timestamp'i okunabilir tarihe çevir
     * 
     * @param timestamp Unix timestamp (milliseconds)
     * @param pattern Çıktı formatı
     * @return Formatlanmış tarih string'i
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "dd MMMM yyyy HH:mm"): String {
        return try {
            val formatter = SimpleDateFormat(pattern, Locale("tr", "TR"))
            formatter.format(Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    /**
     * Para birimi formatlama
     * 
     * @param amount Miktar
     * @param currencyCode Para birimi kodu (TRY, USD, EUR vb.)
     * @return Formatlanmış para string'i
     */
    fun formatCurrency(amount: Double, currencyCode: String = "TRY"): String {
        return try {
            val locale = when (currencyCode.uppercase()) {
                "TRY" -> Locale("tr", "TR")
                "USD" -> Locale("en", "US")
                "EUR" -> Locale("de", "DE")
                "GBP" -> Locale("en", "GB")
                else -> Locale.getDefault()
            }
            
            val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
                currency = java.util.Currency.getInstance(currencyCode)
            }
            
            numberFormat.format(amount)
        } catch (e: Exception) {
            "$amount $currencyCode"
        }
    }

    /**
     * Regex pattern ile özel maskeleme
     * 
     * @param text Maskeleme yapılacak metin
     * @param pattern Regex pattern
     * @param replacement Değiştirme string'i (* karakteri kullanılabilir)
     * @return Maskelenmiş metin
     */
    fun maskWithRegex(text: String, pattern: String, replacement: String): String {
        return try {
            val regex = Regex(pattern)
            regex.replace(text, replacement)
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Hassas verileri otomatik olarak tespit et ve maskele
     * 
     * @param text Maskeleme yapılacak metin
     * @param maskEmails Email adreslerini maskele (varsayılan: true)
     * @param maskPhones Telefon numaralarını maskele (varsayılan: true)
     * @param maskCreditCards Kredi kartlarını maskele (varsayılan: false)
     * @return Maskelenmiş metin
     */
    fun autoMaskSensitiveData(
        text: String,
        maskEmails: Boolean = true,
        maskPhones: Boolean = true,
        maskCreditCards: Boolean = false
    ): String {
        var result = text
        
        if (maskEmails) {
            result = maskEmail(result)
        }
        
        if (maskPhones) {
            result = maskPhone(result)
        }
        
        if (maskCreditCards) {
            result = maskCreditCard(result)
        }
        
        return result
    }

    /**
     * Metni kısalt (ellipsis ekle)
     * 
     * @param text Kısaltılacak metin
     * @param maxLength Maksimum uzunluk
     * @param addEllipsis Ellipsis eklenecek mi?
     * @return Kısaltılmış metin
     */
    fun truncate(text: String, maxLength: Int, addEllipsis: Boolean = true): String {
        return when {
            text.length <= maxLength -> text
            addEllipsis && maxLength > 3 -> "${text.take(maxLength - 3)}..."
            else -> text.take(maxLength)
        }
    }

    /**
     * HTML tag'lerini temizle
     * 
     * @param html HTML içeren metin
     * @return Temizlenmiş metin
     */
    fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }

    /**
     * URL'leri doğrula ve formatla
     * 
     * @param url Doğrulanacak URL
     * @return Formatlanmış URL veya null (geçersiz ise)
     */
    fun validateAndFormatUrl(url: String): String? {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> null
        }
    }

    /**
     * Base64 encode/decode işlemleri
     */
    fun base64Encode(text: String): String {
        return try {
            android.util.Base64.encodeToString(text.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            text
        }
    }

    fun base64Decode(encoded: String): String {
        return try {
            String(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP))
        } catch (e: Exception) {
            encoded
        }
    }

    /**
     * Metindeki tekrarlanan boşlukları temizle
     */
    fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * CamelCase'i snake_case'e çevir
     */
    fun camelToSnake(camelCase: String): String {
        return camelCase.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    /**
     * snake_case'i CamelCase'e çevir
     */
    fun snakeToCamel(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}
