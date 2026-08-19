package com.clippycore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clippycore.app.ui.screens.MainScreen
import com.clippycore.app.viewmodel.MainViewModel

/**
 * MainActivity - Uygulamanın ana aktivitesi
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = viewModel { MainViewModel(application) }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val searchQuery by viewModel.searchQuery.collectAsState()

                    MainScreen(
                        clipboardItems = uiState.clipboardItems,
                        isLoading = uiState.isLoading,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        onItemClick = { item ->
                            // Detay gösterimi için işlem yapılabilir
                        },
                        onFavoriteToggle = { item ->
                            viewModel.toggleFavorite(item)
                        },
                        onDeleteClick = { item ->
                            viewModel.deleteItem(item)
                        },
                        onCopyClick = { item ->
                            // Clipboard'a kopyalama işlemi
                        },
                        onTransformClick = { item ->
                            // Dönüştürme dialog'u gösterilebilir
                        },
                        onRefresh = {
                            // Yenileme işlemi
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel otomatik olarak temizlenir
    }
}
