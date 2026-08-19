package com.clippycore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clippycore.app.data.database.ClipboardItem
import com.clippycore.app.util.TextTransformer

/**
 * Ana Ekran (MainScreen) - Jetpack Compose ile yazılmış kullanıcı arayüzü
 * 
 * Özellikler:
 * - Clipboard geçmişi listesi
 * - Arama fonksiyonu
 * - Favori işaretleme
 * - İçerik detay görüntüleme
 * - Dönüştürme işlemleri (JSON format, maskeleme vb.)
 */
@Composable
fun MainScreen(
    clipboardItems: List<ClipboardItem>,
    isLoading: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onItemClick: (ClipboardItem) -> Unit = {},
    onFavoriteToggle: (ClipboardItem) -> Unit = {},
    onDeleteClick: (ClipboardItem) -> Unit = {},
    onCopyClick: (ClipboardItem) -> Unit = {},
    onTransformClick: (ClipboardItem) -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFavoritesOnly by remember { mutableStateOf(false) }
    
    // Filtrelenmiş liste
    val filteredItems = remember(clipboardItems, searchQuery, showFavoritesOnly) {
        clipboardItems.filter { item ->
            // Favori filtresi
            if (showFavoritesOnly && !item.isFavorite) return@filter false
            
            // Arama filtresi
            if (searchQuery.isNotBlank()) {
                item.content.contains(searchQuery, ignoreCase = true) ||
                (item.transformedContent?.contains(searchQuery, ignoreCase = true) == true)
            } else {
                true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Üst Bar - Başlık ve Aksiyonlar
        TopAppBar(
            title = {
                Text(
                    text = "Pano Geçmişi",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // Favori filtresi toggle
                IconButton(onClick = { showFavoritesOnly = !showFavoritesOnly }) {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favoriler",
                        tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Yenile butonu
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Yenile"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Arama Barı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text("Panoda ara...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ara"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )
        }

        // İstatistikler
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Toplam: ${filteredItems.size} öğe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (showFavoritesOnly) {
                Text(
                    text = "(Favoriler gösteriliyor)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Liste veya Boş Durum
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredItems.isEmpty()) {
            EmptyStateView(
                hasSearchQuery = searchQuery.isNotEmpty(),
                isFilteredByFavorites = showFavoritesOnly,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ClipboardItemCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onFavoriteToggle = { onFavoriteToggle(item) },
                        onDeleteClick = { onDeleteClick(item) },
                        onCopyClick = { onCopyClick(item) },
                        onTransformClick = { onTransformClick(item) }
                    )
                }
            }
        }
    }
}

/**
 * Clipboard Item Card - Her bir pano öğesini gösteren kart
 */
@Composable
private fun ClipboardItemCard(
    item: ClipboardItem,
    onClick: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onTransformClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isFavorite) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Üst satır - Tip ve zaman
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // İçerik tipi badge
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = item.itemType.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = getIconForType(item.itemType),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    border = null,
                    modifier = Modifier.height(24.dp)
                )
                
                // Zaman
                Text(
                    text = TextTransformer.formatTimestamp(item.timestamp, "dd MMM HH:mm"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // İçerik önizleme
            Text(
                text = TextTransformer.truncate(item.content, maxLength = 150),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Karakter sayısı
            if (item.characterCount > 50) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.characterCount} karakter",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Aksiyon butonları
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Favori toggle
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Kopyala
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Dönüştür
                IconButton(
                    onClick = onTransformClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = "Dönüştür",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Sil
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Boş durum görünümü
 */
@Composable
private fun EmptyStateView(
    hasSearchQuery: Boolean,
    isFilteredByFavorites: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = when {
                    hasSearchQuery -> Icons.Default.SearchOff
                    isFilteredByFavorites -> Icons.Outlined.FavoriteBorder
                    else -> Icons.Default.ContentPasteOff
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    hasSearchQuery -> "Arama sonucu bulunamadı"
                    isFilteredByFavorites -> "Henüz favori öğe yok"
                    else -> "Pano geçmişi boş"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    hasSearchQuery -> "Farklı bir arama terimi deneyin"
                    isFilteredByFavorites -> "Favorilere eklenen öğeler burada görünecek"
                    else -> "Kopyaladığınız içerikler burada görünecek"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * İçerik tipine göre icon seçimi
 */
@Composable
private fun getIconForType(type: ClipboardItem.ItemType) = when (type) {
    ClipboardItem.ItemType.JSON -> Icons.Default.Code
    ClipboardItem.ItemType.EMAIL -> Icons.Default.Email
    ClipboardItem.ItemType.PHONE -> Icons.Default.Phone
    ClipboardItem.ItemType.URL -> Icons.Default.Link
    ClipboardItem.ItemType.DATE -> Icons.Default.CalendarToday
    ClipboardItem.ItemType.TIME -> Icons.Default.AccessTime
    ClipboardItem.ItemType.CURRENCY -> Icons.Default.AttachMoney
    ClipboardItem.ItemType.XML -> Icons.Default.Code
    ClipboardItem.ItemType.CODE_SNIPPET -> Icons.Default.Terminal
    ClipboardItem.ItemType.PLAIN_TEXT,
    ClipboardItem.ItemType.ADDRESS -> Icons.Default.TextFields
}

/**
 * Detay Dialog'u - Seçili öğenin tam içeriğini gösterir
 */
@Composable
fun ItemDetailDialog(
    item: ClipboardItem,
    onDismiss: () -> Unit,
    onCopyClick: () -> Unit = {},
    onTransformClick: (transformType: TransformType) -> Unit = {}
) {
    var showTransformOptions by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = getIconForType(item.itemType),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.itemType.name,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Tam içerik
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Meta bilgiler
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        InfoRow(label = "Karakter", value = item.characterCount.toString())
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow(label = "Zaman", value = TextTransformer.formatTimestamp(item.timestamp))
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow(label = "Kaynak", value = item.sourcePackage ?: "Bilinmiyor")
                    }
                }
                
                // Dönüştürme seçenekleri
                if (showTransformOptions) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Dönüştürme İşlemleri:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.itemType == ClipboardItem.ItemType.JSON) {
                            FilterChip(
                                selected = false,
                                onClick = { onTransformClick(TransformType.FORMAT_JSON) },
                                label = { Text("JSON Formatla") },
                                leadingIcon = {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                                }
                            )
                        }
                        
                        FilterChip(
                            selected = false,
                            onClick = { onTransformClick(TransformType.MASK_SENSITIVE) },
                            label = { Text("Maskele") },
                            leadingIcon = {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCopyClick) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kopyala")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (showTransformOptions) {
                    showTransformOptions = false
                } else {
                    onDismiss()
                }
            }) {
                if (showTransformOptions) "Geri" else "Kapat"
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class TransformType {
    FORMAT_JSON,
    MASK_SENSITIVE,
    MASK_EMAIL,
    MASK_PHONE,
    FORMAT_DATE
}
