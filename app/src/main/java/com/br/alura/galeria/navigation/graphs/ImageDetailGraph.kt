package com.br.alura.galeria.navigation.graphs

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.br.alura.galeria.R
import com.br.alura.galeria.extensions.cleanBrackets
import com.br.alura.galeria.mlkit.ImageClassifier
import com.br.alura.galeria.navigation.Destinations
import com.br.alura.galeria.ui.imageDetail.ImageDetailScreen


fun NavGraphBuilder.imageDetailGraph() {
    composable(
        route = Destinations.ImageDetail.route
    ) {
        val context = LocalContext.current

        var description by remember {
            mutableStateOf("#descrição #da #imagem")
        }

        val imageBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.cars
        )

        var currentImage: Any by remember {
            mutableStateOf(imageBitmap)
        }
        //val image: InputImage = InputImage.fromBitmap(imageBitmap, 0)

        val imageClassifier = ImageClassifier(context)


        ImageDetailScreen(
            defaultImage = currentImage,
            description = description,
            onImageChange = {
                currentImage = it
                imageClassifier.classifyImage(
                    imageUri = it.toString(),
                    onSuccess = { labels ->
                        description = labels.toString().cleanBrackets()
                    },
                    onFailure = {
                        Log.e("ImageDetailScreen", it.message.toString())
                    }
                )
            }
        )
    }
}