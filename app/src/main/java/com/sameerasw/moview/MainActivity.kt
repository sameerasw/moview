package com.sameerasw.moview

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Import viewModels delegate
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
import com.sameerasw.moview.ui.theme.MoviewTheme

// We will define the ViewModel later
// import com.sameerasw.moview.data.MainViewModel

class MainActivity : ComponentActivity() {

    // Initialize ViewModel (We'll create this class soon)
    // private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the ViewModel's function to the screen
                    MainScreen(
                        onAddMoviesClicked = {
                            println("Add Movies Button Clicked - Calling ViewModel soon...")
                            // viewModel.addInitialMoviesToDb() // We will uncomment this later
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(onAddMoviesClicked: () -> Unit) { // Accept lambda for the action

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onAddMoviesClicked) { // Call the passed lambda
            Text("Add Movies to DB")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Get context inside onClick where it's needed
        val context = LocalContext.current // Can still get it here for reuse below

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
        // Task 7 Button will go here later
    }
}