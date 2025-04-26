package com.sameerasw.moview

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sameerasw.moview.data.Movie
import com.sameerasw.moview.data.MovieDatabase
import com.sameerasw.moview.ui.theme.MoviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    private val movieDao by lazy {
        MovieDatabase.getDatabase(applicationContext).movieDao()
    }

    // URL specified
    private val moviesDataUrl = "https://ddracopo.github.io/DOCUM/courses/5cosc023w/movies.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onAddMoviesClicked = { addInitialMoviesToDb() }
                    )
                }
            }
        }
    }

    private fun addInitialMoviesToDb() {
        lifecycleScope.launch {
            try {
                val moviesToAdd = fetchAndParseMovies(moviesDataUrl)

                if (moviesToAdd.isNotEmpty()) {
                    // Insert into DB on IO thread
                    withContext(Dispatchers.IO) {
                        movieDao.insertMovies(moviesToAdd)
                    }
                    // Show success message on Main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "${moviesToAdd.size} Movies added to DB",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    println("Successfully added ${moviesToAdd.size} movies to DB.")

                    val allMovies = withContext(Dispatchers.IO) { movieDao.getAllMovies() }
                    println("Current movies in DB: ${allMovies.size}")

                } else {
                    // Show error
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "No movies found or error fetching data.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    println("No movies parsed or error fetching data.")
                }

            } catch (e: Exception) {
                // Show error message on Main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
                println("Error during movie fetch/DB operation: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchAndParseMovies(urlString: String): List<Movie> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<Movie>()
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        val currentMovieData = mutableMapOf<String, String>()
        var currentKey: String? = null
        val currentValue = StringBuilder()

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 10000
            connection.connect()

            val inputStream = connection.inputStream
            reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()

                if (trimmedLine.isEmpty()) {
                    if (currentKey != null && currentValue.isNotEmpty()) {
                        currentMovieData[currentKey!!] = currentValue.toString().trim().removeSurrounding("\"")
                    }

                    if (currentMovieData.isNotEmpty()) {
                        try {
                            println("Attempting to create movie from map: $currentMovieData")
                            val movie = createMovieFromMap(currentMovieData)
                            movies.add(movie)
                            println("Successfully created movie: ${movie.title}")
                        } catch (e: Exception) {
                            println("Error creating movie from map: ${e.message} - Data: $currentMovieData")
                        }
                        currentMovieData.clear()
                    }
                    currentKey = null
                    currentValue.clear()

                } else {
                    val colonIndex = trimmedLine.indexOf(':')
                    if (colonIndex != -1) {
                        //key - valuue
                        if (currentKey != null && currentValue.isNotEmpty()) {
                            currentMovieData[currentKey!!] = currentValue.toString().trim().removeSurrounding("\"")
                        }

                        currentKey = trimmedLine.substring(0, colonIndex).trim().removeSurrounding("\"")
                        currentValue.clear()
                        currentValue.append(trimmedLine.substring(colonIndex + 1).trim())

                    } else {
                        if (currentKey != null) {
                            if (currentValue.isNotEmpty()) currentValue.append(" ")
                            currentValue.append(trimmedLine)
                        } else {
                            println("Skipping line with no colon and no current key: $trimmedLine")
                        }
                    }
                }
            }

            if (currentKey != null && currentValue.isNotEmpty()) {
                currentMovieData[currentKey!!] = currentValue.toString().trim().removeSurrounding("\"")
            }
            if (currentMovieData.isNotEmpty()) {
                try {
                    println("Attempting to create LAST movie from map: $currentMovieData")
                    val movie = createMovieFromMap(currentMovieData)
                    movies.add(movie)
                    println("Successfully created LAST movie: ${movie.title}")
                } catch (e: Exception) {
                    println("Error creating LAST movie from map: ${e.message} - Data: $currentMovieData")
                }
            }

        } catch (e: Exception) {
            println("Error during network fetch or reading: ${e.message}")
            e.printStackTrace()
            return@withContext emptyList<Movie>()
        } finally {
            reader?.close()
            connection?.disconnect()
        }
        println("Finished Parsing. Total movies parsed successfully: ${movies.size}")
        return@withContext movies
    }

    private fun createMovieFromMap(data: Map<String, String>): Movie {
        val imdbIDValue = data["imdbID"]
        return Movie(
            title = data["Title"],
            year = data["Year"],
            rated = data["Rated"],
            released = data["Released"],
            runtime = data["Runtime"],
            genre = data["Genre"],
            director = data["Director"],
            writer = data["Writer"],
            actors = data["Actors"],
            plot = data["Plot"],
            poster = data["Poster"],
            imdbRating = data["imdbRating"],
            type = data["Type"]
        )
    }
}

@Composable
fun MainScreen(onAddMoviesClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onAddMoviesClicked) {
            Text("Add Movies to DB")
        }
        Spacer(modifier = Modifier.height(16.dp))
        val context = LocalContext.current
        Button(onClick = {
            val intent = Intent(context, SearchMoviesActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Search for Movies")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val intent = Intent(context, SearchActorsActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Search for Actors")
        }
        // TODO: Task 7
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Implement Task 7 action */ }) {
            Text("Search Title (Web)")
        }
    }
}