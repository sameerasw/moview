package com.sameerasw.moview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.moview.ui.theme.MoviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

// to hold search results
data class WebSearchResult(
    val title: String?,
    val year: String?,
    val type: String?,
    val poster: String?
)

class SearchTitleWebActivity : ComponentActivity() {

    private val apiKey = "19b1fdf9" // API key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (apiKey == "YOUR_API_KEY") {
            Toast.makeText(this, "Please replace 'YOUR_API_KEY' in SearchTitleWebActivity.kt", Toast.LENGTH_LONG).show()
        }
        setContent {
            MoviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchTitleWebScreen(
                        apiKey = apiKey,
                        searchAction = { searchTerm -> searchMoviesBySubstring(searchTerm, apiKey) }
                    )
                }
            }
        }
    }

    // Searches OMDb
    private suspend fun searchMoviesBySubstring(
        searchTerm: String,
        key: String
    ): Result<List<WebSearchResult>> = withContext(Dispatchers.IO) {
        if (key == "YOUR_API_KEY" || key.isBlank()) {
            return@withContext Result.failure(Exception("API Key not set"))
        }
        if (searchTerm.isBlank()) {
            return@withContext Result.failure(Exception("Search term cannot be empty"))
        }

        val encodedSearchTerm = URLEncoder.encode(searchTerm, "UTF-8")
        // s substring search
        val urlString = "https://www.omdbapi.com/?s=$encodedSearchTerm&apikey=$key"
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 10000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                println("OMDb Substring Search Response: $response")

                val jsonObject = JSONObject(response)

                if (jsonObject.optString("Response") == "False") {
                    return@withContext Result.failure(Exception(jsonObject.optString("Error", "No results found")))
                }

                // Parse array
                val searchArray = jsonObject.optJSONArray("Search")
                val results = mutableListOf<WebSearchResult>()
                if (searchArray != null) {
                    for (i in 0 until minOf(searchArray.length(), 10)) { // Limit to 10
                        val movieObject = searchArray.getJSONObject(i)
                        results.add(
                            WebSearchResult(
                                title = movieObject.optString("Title", null),
                                year = movieObject.optString("Year", null),
                                type = movieObject.optString("Type", null),
                                poster = movieObject.optString("Poster", null)
                            )
                        )
                    }
                }
                println("Parsed ${results.size} results.")
                return@withContext Result.success(results)

            } else {
                return@withContext Result.failure(Exception("OMDb API error: $responseCode"))
            }

        } catch (e: Exception) {
            println("Error during OMDb substring search: ${e.message}")
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }
}

@Composable
fun SearchTitleWebScreen(
    apiKey: String,
    searchAction: suspend (String) -> Result<List<WebSearchResult>>
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchResults by rememberSaveable { mutableStateOf<List<WebSearchResult>>(emptyList()) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var searchPerformed by rememberSaveable { mutableStateOf(false) }

    val composableScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                .asPaddingValues())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search Term (e.g., 'matrix', 'dark knight')") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isLoading = true
                    searchPerformed = true
                    errorMessage = null
                    searchResults = emptyList() // Clear previous results
                    composableScope.launch {
                        val result = searchAction(searchText)
                        isLoading = false
                        result.onSuccess { results ->
                            searchResults = results
                        }.onFailure { error ->
                            errorMessage = error.message ?: "Unknown search error"
                        }
                    }
                },
                enabled = !isLoading && searchText.isNotBlank() && apiKey != "YOUR_API_KEY"
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }
            searchPerformed && searchResults.isEmpty() -> {
                // Empty
                Text("No results found for '$searchText'.")
            }
            searchResults.isNotEmpty() -> {
                // Display Results
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { result ->
                        SearchResultItem(result = result)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(result: WebSearchResult) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageLoaded by remember { mutableStateOf(false) }
    val posterUrl = result.poster
    val coroutineScope = rememberCoroutineScope()

    // Load poster image
    LaunchedEffect(posterUrl) {
        if (!posterUrl.isNullOrEmpty() && posterUrl != "N/A") {
            coroutineScope.launch {
                bitmap = downloadBitmap(posterUrl)
                imageLoaded = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(180.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // Display poster as background
        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.5f),
                contentScale = ContentScale.Crop
            )
        }

        // Movie details
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.BottomStart)
        ) {
            Text(
                result.title ?: "No Title",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Year: ${result.year ?: "N/A"}, Type: ${result.type ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// download image bitmaps
private suspend fun downloadBitmap(url: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Out of scope references helped with fetchign and displaying images:
// BitmapFactory: https://developer.android.com/reference/android/graphics/BitmapFactory
// ImageView.setImageBitmap(): https://developer.android.com/reference/android/widget/ImageView#setImageBitmap(android.graphics.Bitmap)
// https://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
