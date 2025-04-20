package com.sameerasw.moview

import android.content.Intent
import android.os.Bundle
import android.widget.Toast // Import Toast
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

class MainActivity : ComponentActivity() {

    //  initialize the database
    private val movieDao by lazy {
        MovieDatabase.getDatabase(applicationContext).movieDao()
    }

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
            val moviesToAdd = listOf(
                Movie(
                    imdbID = "tt0111161", // Primary Key
                    title = "The Shawshank Redemption",
                    year = "1994",
                    rated = "R",
                    released = "14 Oct 1994",
                    runtime = "142 min",
                    genre = "Drama",
                    director = "Frank Darabont",
                    writer = "Stephen King, Frank Darabont",
                    actors = "Tim Robbins, Morgan Freeman, Bob Gunton",
                    plot = "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.",
                    poster = "https://m.media-amazon.com/images/M/MV5BNDE3ODcxYzMtY2YzZC00NmNlLWJiNDMtZDViZWM2MzIxZDYwXkEyXkFqcGdeQXVyNjAwNDUxODI@._V1_SX300.jpg", // Poster URL
                    imdbRating = "9.3",
                    type = "movie"
                ),
                Movie(
                    imdbID = "tt0068646",
                    title = "The Godfather",
                    year = "1972",
                    rated = "R",
                    released = "24 Mar 1972",
                    runtime = "175 min",
                    genre = "Crime, Drama",
                    director = "Francis Ford Coppola",
                    writer = "Mario Puzo, Francis Ford Coppola",
                    actors = "Marlon Brando, Al Pacino, James Caan",
                    plot = "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.",
                    poster = "https://m.media-amazon.com/images/M/MV5BM2MyNjYxNmUtYTAwNi00MTYxLWJmNWYtYzZlODY3ZTk3OTFlXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
                    imdbRating = "9.2",
                    type = "movie"
                )
            )

            try {
                // ddb insertion on a background thread (IO Dispatcher)
                withContext(Dispatchers.IO) {
                    movieDao.insertMovies(moviesToAdd)
                }
                Toast.makeText(this@MainActivity, "Movies added to DB", Toast.LENGTH_SHORT).show()
                println("Successfully added ${moviesToAdd.size} movies to DB.")

                val allMovies = withContext(Dispatchers.IO) { movieDao.getAllMovies() }
                println("Current movies in DB: ${allMovies.size}")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error adding movies: ${e.message}", Toast.LENGTH_LONG).show()
                }
                println("Error adding movies to DB: ${e.message}")
            }
        }
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
        // Task 7 Button
    }
}