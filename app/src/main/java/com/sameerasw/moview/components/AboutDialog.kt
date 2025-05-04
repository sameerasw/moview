package com.sameerasw.moview.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sameerasw.moview.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit,
    appName: String = "App",
    developerName: String = "Developer",
    description: String = "",
    githubUsername: String = "",
    showPlagiarismStatement: Boolean = true
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("About $appName") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Profile picture
                Image(
                    painter = painterResource(id = R.drawable.avatar),
                    contentDescription = "Developer Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Developed by $developerName",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(description)

                if (showPlagiarismStatement) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "I confirm that I understand what plagiarism is and have read and " +
                                "understood the section on Assessment Offences in the Essential Information for Students. " +
                                "The work that I have submitted is entirely my own. Any work from other authors " +
                                "is duly referenced and acknowledged."
                    )
                }
            }
        },
        dismissButton = {
            if (githubUsername.isNotBlank()) {
                IconButton(onClick = {
                    val githubUrl = "https://github.com/$githubUsername"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                    context.startActivity(intent)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.github),
                        contentDescription = "GitHub Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("OK")
            }
        }
    )
}