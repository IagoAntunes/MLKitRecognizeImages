package com.br.alura.galeria.ui.splashScreen

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.alura.galeria.ImageUriManager
import com.br.alura.galeria.data.ImageWithLabels
import com.br.alura.galeria.dataStore.UserPreferencesDataStore
import com.br.alura.galeria.mlkit.ImageClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
    private val imageUriManager: ImageUriManager,
    private val imageClassifier: ImageClassifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashScreenUiState())
    val uiState: StateFlow<SplashScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadStartDestination()
        }
    }

    private suspend fun loadStartDestination() {
        dataStore.getLastSelectedPath()?.let { previousPath ->
            _uiState.value = _uiState.value.copy(
                path = previousPath
            )
            loadImages()
        } ?: run {
            _uiState.value = _uiState.value.copy(
                appState = AppState.FirstTime,
            )
        }
    }


    private fun loadImages() {
        _uiState.value.path?.let { path ->
            viewModelScope.launch {
                imageUriManager.loadImagesFromFolder(
                    folderPath = path,
                    onLoaded = {
                        classifyImages()
                    }
                )
            }
        }
    }

    private fun classifyImages() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val imageUris = imageUriManager.getImageUris()
                val imagesWithLabelsDeferred = mutableListOf<Deferred<ImageWithLabels>>()

                imageUris.forEach { imageUri ->
                    val deferred = CompletableDeferred<ImageWithLabels>()
                    imageClassifier.classifyImage(
                        imageUri = imageUri,
                        onSuccess = { labels ->
                            deferred.complete(ImageWithLabels(uri = imageUri, labels = labels))
                        },
                        onFailure = {
                            Log.e("ERROR ClassifyImage", it.message.toString())
                        }
                    )
                    imagesWithLabelsDeferred.add(deferred)

                }
                val imagesWithLabels = imagesWithLabelsDeferred.awaitAll()
                imageUriManager.setImageWithLabels(imagesWithLabels)
                _uiState.value = _uiState.value.copy(
                    appState = AppState.Loaded,
                    route = Route.GALLERY
                )

            }
        }
    }
}