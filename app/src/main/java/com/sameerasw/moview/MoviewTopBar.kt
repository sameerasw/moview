package com.sameerasw.moview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviewTopBar(title: String = "Dicing") {
    // https://developer.android.com/develop/ui/compose/components/app-bars
    // Top app bar with the app icon and title
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App",
                    modifier = Modifier
                        .padding(
                            horizontal = 4.dp,
                            vertical = 8.dp
                        )
                        .size(64.dp)
                        .scale(1.5f),
                    tint = Color(MaterialTheme.colorScheme.primary.value)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).value),
        )
    )
}