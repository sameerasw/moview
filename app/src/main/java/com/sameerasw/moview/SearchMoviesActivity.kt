package com.sameerasw.moview

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.moview.components.MoviePoster
import com.sameerasw.moview.components.SearchField
import com.sameerasw.moview.components.SearchStateDisplay
import com.sameerasw.moview.data.Movie
import com.sameerasw.moview.data.MovieDatabase
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

class SearchMoviesActivity : ComponentActivity() {

    // OMDb API Key
    private val apiKey = "19b1fdf9"

    private val movieDao by lazy {
        MovieDatabase.getDatabase(applicationContext).movieDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (apiKey == "YOUR_API_KEY") {
            Toast.makeText(this, "Please replace 'YOUR_API_KEY' in SearchMoviesActivity.kt", Toast.LENGTH_LONG).show()
        }
        setContent {
            MoviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchMoviesScreen(
                        apiKey = apiKey,
                        searchAction = { title -> fetchMovieFromApi(title, apiKey) },
                        saveAction = { movie -> saveMovieToDb(movie) }
                    )
                }
            }
        }
    }

    // Fetches moviie details, suspend is used to call this function from a coroutine
    private suspend fun fetchMovieFromApi(title: String, key: String): Result<Movie> = withContext(Dispatchers.IO) {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val urlString = "https://www.omdbapi.com/?t=$encodedTitle&apikey=$key"
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
                Log.d("OMDb Response", response)

                val jsonObject = JSONObject(response)

                if (jsonObject.optString("Response") == "False") {
                    return@withContext Result.failure(Exception(jsonObject.optString("Error", "Movie not found")))
                }

                // Parse JSON
                val movie = Movie(
                    title = jsonObject.optString("Title", null),
                    year = jsonObject.optString("Year", null),
                    rated = jsonObject.optString("Rated", null),
                    released = jsonObject.optString("Released", null),
                    runtime = jsonObject.optString("Runtime", null),
                    genre = jsonObject.optString("Genre", null),
                    director = jsonObject.optString("Director", null),
                    writer = jsonObject.optString("Writer", null),
                    actors = jsonObject.optString("Actors", null),
                    plot = jsonObject.optString("Plot", null),
                    poster = jsonObject.optString("Poster", null),
                    imdbRating = jsonObject.optString("imdbRating", null),
                    type = jsonObject.optString("Type", null)
                )
                Log.d("Parsed Movie", movie.toString())
                return@withContext Result.success(movie)

            } else {
                return@withContext Result.failure(Exception("OMDb API error: $responseCode"))
            }

        } catch (e: Exception) {
            Log.e("Error fetching/parsing OMDb", e.message ?: "Unknown error")
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }

    // Saves the movie to the Room database
    private suspend fun saveMovieToDb(movie: Movie): Boolean = withContext(Dispatchers.IO) {
        try {
            movieDao.insertMovie(movie)
            return@withContext true
        } catch (e: Exception) {
            Log.e("Error saving movie to DB", e.message ?: "Unknown error")
            e.printStackTrace()
            return@withContext false // return false if an error occurs
        }
    }
}

@Composable
fun SearchMoviesScreen(
    apiKey: String,
    searchAction: suspend (String) -> Result<Movie>,
    saveAction: suspend (Movie) -> Boolean
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var lastSearchedTerm by rememberSaveable { mutableStateOf("") }
    var movieDetails by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchPerformed by remember { mutableStateOf(false) }

    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Effect to re-fetch data
    LaunchedEffect(Unit) {
        if (lastSearchedTerm.isNotBlank()) {
            isLoading = true
            errorMessage = null
            movieDetails = null

            val result = searchAction(lastSearchedTerm)
            isLoading = false
            searchPerformed = true
            result.onSuccess { movie ->
                movieDetails = movie
            }.onFailure { error ->
                errorMessage = error.message ?: "Unknown error fetching movie"
            }
        }
    }

    val performSearch = {
        if (searchText.isNotBlank() && apiKey != "YOUR_API_KEY") {
            lastSearchedTerm = searchText
            isLoading = true
            errorMessage = null
            movieDetails = null

            composableScope.launch {
                val result = searchAction(searchText)
                isLoading = false
                searchPerformed = true
                result.onSuccess { movie ->
                    movieDetails = movie
                }.onFailure { error ->
                    errorMessage = error.message ?: "Unknown error fetching movie"
                }
            }
        } else if (apiKey == "YOUR_API_KEY") {
            errorMessage = "Please set your OMDb API key."
        } else {
            errorMessage = "Please enter a movie title."
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                .asPaddingValues())
    ) {
        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 0.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Display movie details or search state
            movieDetails?.let { movie ->
                Column {
                    MovieDetailDisplay(movie = movie)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            composableScope.launch {
                                val success = saveAction(movie)
                                if (success) {
                                    Toast.makeText(context, "${movie.title} saved to database", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error saving movie", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_save),
                            contentDescription = "Save",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Movie to Database")
                    }
                }
            } ?: SearchStateDisplay(
                isLoading = isLoading,
                errorMessage = errorMessage,
                emptySearchMessage = "Search for a movie by title to see details",
                searchPerformed = searchPerformed,
                hasResults = movieDetails != null
            )
        }

        // Search bar
        SearchField(
            value = searchText,
            onValueChange = { searchText = it },
            onSearch = { performSearch() },
            label = "Movie Title",
            enabled = !isLoading,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MovieDetailDisplay(movie: Movie) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MoviePoster(
                posterUrl = movie.poster,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(movie.title ?: "No Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                DetailRow("Year:", movie.year)
                DetailRow("Rated:", movie.rated)
                DetailRow("Released:", movie.released)
                DetailRow("Runtime:", movie.runtime)
                DetailRow("IMDb Rating:", movie.imdbRating)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        DetailRow("Genre:", movie.genre)
        DetailRow("Director:", movie.director)
        DetailRow("Writer:", movie.writer)
        DetailRow("Actors:", movie.actors)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Plot:", style = MaterialTheme.typography.titleMedium)
        Text(movie.plot ?: "N/A")
    }
}

@Composable
fun DetailRow(label: String, value: String?) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp)) // fixed width to align the labels
        Text(value ?: "N/A")
    }
}