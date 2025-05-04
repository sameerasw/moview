package com.sameerasw.moview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class SearchActorsActivity : ComponentActivity() {

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
                    // Pass the DAO search function, lambdda func
                    SearchActorsScreen { actorName ->
                        findMoviesByActorSuspend(actorName)
                    }
                }
            }
        }
    }

    // call DAO on IO thread
    private suspend fun findMoviesByActorSuspend(actorName: String): List<Movie> {
        return withContext(Dispatchers.IO) {
            movieDao.findMoviesByActor(actorName)
        }
    }
}

@Composable
fun SearchActorsScreen(
    searchAction: suspend (String) -> List<Movie>
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var moviesResult by rememberSaveable { mutableStateOf<List<Movie>>(emptyList()) }
    var searchPerformed by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val composableScope = rememberCoroutineScope()

    val performSearch = {
        if (searchText.isNotBlank()) {
            searchPerformed = true
            isLoading = true
            composableScope.launch {
                moviesResult = searchAction(searchText)
                isLoading = false
            }
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
        ) {
            if (moviesResult.isNotEmpty()) {
                LazyColumn {
                    items(moviesResult) { movie ->
                        MovieActorItem(movie = movie)
                    }
                }
            } else {
                SearchStateDisplay(
                    isLoading = isLoading,
                    emptySearchMessage = "Search for an actor to find their movies",
                    searchPerformed = searchPerformed,
                    hasResults = moviesResult.isNotEmpty()
                )
            }
        }

        // Search bar
        SearchField(
            value = searchText,
            onValueChange = { searchText = it },
            onSearch = { performSearch() },
            label = "Actor Name",
            buttonText = "Search",
            enabled = !isLoading,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MovieActorItem(movie: Movie) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MoviePoster(
            posterUrl = movie.poster,
            modifier = Modifier.size(100.dp, 150.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = movie.title ?: "No Title", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Year: ${movie.year ?: "N/A"}")
            Text(text = "Actors: ${movie.actors ?: "N/A"}")
            if (!movie.genre.isNullOrEmpty()) {
                Text(text = "Genre: ${movie.genre}")
            }
            if (!movie.imdbRating.isNullOrEmpty()) {
                Text(text = "Rating: ${movie.imdbRating}")
            }
        }
    }
}