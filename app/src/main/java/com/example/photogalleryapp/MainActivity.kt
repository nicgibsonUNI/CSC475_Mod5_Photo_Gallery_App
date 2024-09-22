package com.example.photogalleryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.CachePolicy
import com.example.photogalleryapp.ui.theme.PhotoGalleryAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoGalleryAppTheme {
                PhotoGrid()
            }
        }
    }
}

@Composable
fun PhotoGrid() {
    var imageUrls by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // ImageLoader to handle image caching
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    // Function to load images
    fun loadImages() {
        if (!isLoading) {
            isLoading = true
            coroutineScope.launch {
                val newImages = fetchImagesFromApi(2)
                if (newImages.isNotEmpty()) {
                    imageUrls = newImages
                }
                isLoading = false
            }
        }
    }

    // Fetch initial set of images when the composable is first displayed
    LaunchedEffect(key1 = Unit) {
        loadImages()
    }

    // Display grid and loading indicator
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),  // Display 2 images across
            modifier = Modifier.fillMaxSize(),
            content = {
                items(imageUrls.size) { index ->
                    LoadImage(url = imageUrls[index], imageLoader = imageLoader)
                }
            }
        )

        // Show a loading spinner when images are loading
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

// Loading images
@Composable
fun LoadImage(url: String, imageLoader: ImageLoader) {
    AsyncImage(
        model = url,
        imageLoader = imageLoader,  // Provide custom ImageLoader
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        error = painterResource(R.drawable.placeholder_image)
    )
}

// Function to fetch images from the Picsum API using Retrofit
suspend fun fetchImagesFromApi(page: Int): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val imageList = RetrofitInstance.api.getImages(page = page, limit = 20)
            imageList.map { it.download_url }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Failed to fetch images", e)
            listOf()  // Return an empty list if there is an error
        }
    }
}

// Data model for the image from Picsum API
data class ImageModel(
    val id: String,
    val download_url: String
)

// Retrofit service interface to fetch image data
interface PicsumService {
    @GET("v2/list")
    suspend fun getImages(
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int
    ): List<ImageModel>
}

// Object to initialize Retrofit
object RetrofitInstance {
    val api: PicsumService by lazy {
        Retrofit.Builder()
            .baseUrl("https://picsum.photos/")
            .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON deserialization
            .build()
            .create(PicsumService::class.java)
    }
}
