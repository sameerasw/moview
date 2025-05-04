package com.sameerasw.moview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.res.painterResource
import com.sameerasw.moview.components.AboutDialog

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
                        onAddMoviesClicked = { addInitialMoviesToDb() },
                        onClearDatabaseClicked = { clearMoviesDatabase() }
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

    private fun clearMoviesDatabase() {
        lifecycleScope.launch {
            try {
                // Delete all movies
                withContext(Dispatchers.IO) {
                    movieDao.deleteAllMovies()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Movie database cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                println("Successfully cleared the movie database")
            } catch (e: Exception) {
                // Show error
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
                println("Error clearing database: ${e.message}")
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
                        currentMovieData[currentKey] = currentValue.toString().trim().removeSurrounding("\"")
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
                            currentMovieData[currentKey] = currentValue.toString().trim().removeSurrounding("\"")
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
                currentMovieData[currentKey] = currentValue.toString().trim().removeSurrounding("\"")
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
fun MainScreen(onAddMoviesClicked: () -> Unit, onClearDatabaseClicked: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MoviewTopBar(title = "Moview")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val context = LocalContext.current

                // Grid of feature buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Add Movies button
                    MainMenuButton(
                        icon = R.drawable.ic_add_movies,
                        label = "Add Movies",
                        onClick = onAddMoviesClicked
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // Search Movies button
                    MainMenuButton(
                        icon = R.drawable.ic_search_movies,
                        label = "Search Movies",
                        onClick = {
                            val intent = Intent(context, SearchMoviesActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Search Actors button
                    MainMenuButton(
                        icon = R.drawable.ic_search_actors,
                        label = "Search Actors",
                        onClick = {
                            val intent = Intent(context, SearchActorsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // Search Web button
                    MainMenuButton(
                        icon = R.drawable.ic_search_web,
                        label = "Search Web",
                        onClick = {
                            val intent = Intent(context, SearchTitleWebActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Bottom buttons - footer
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Reset Database")
                }

                OutlinedButton(
                    onClick = { showAboutDialog = true }
                ) {
                    Text("About")
                }
            }
        }

        // Dialogs
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Reset") },
                text = { Text("Are you sure you want to clear all movies from the database? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearDatabaseClicked()
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    Button(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // about me
        if (showAboutDialog) {
            AboutDialog(
                onDismissRequest = { showAboutDialog = false },
                appName = "Moview",
                developerName = "Sameera Wijerathna",
                description = "Moview is a movie database application that allows users to search and save movie information. " +
                        "The app uses the OMDb API to fetch movie details.",
                githubUsername = "sameerasw"
            )
        }
    }
}

@Composable
fun MainMenuButton(
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}