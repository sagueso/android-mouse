package ve.com.sagues.rafael.android_mouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ve.com.sagues.rafael.android_mouse.ui.theme.AndroidmouseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidmouseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GestureTracker(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GestureTracker(modifier: Modifier = Modifier) {
    // State for instantaneous drag amount (dx, dy)
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }

    // State for accumulated offset (makes it easier to visualize the movement)
    var totalX by remember { mutableFloatStateOf(0f) }
    var totalY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize() // Takes up the whole screen to detect gestures anywhere
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Optional: Reset dx and dy to 0 when the user lifts their finger
                        dx = 0f
                        dy = 0f
                    }
                ) { change, dragAmount ->
                    change.consume() // Consume the event so it isn't passed down to other elements

                    // Update instantaneous dx and dy
                    dx = dragAmount.x
                    dy = dragAmount.y

                    // Update total accumulated positions
                    totalX += dragAmount.x
                    totalY += dragAmount.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Drag anywhere on screen!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Print the values to the screen
            Text(text = "Current dx: $dx")
            Text(text = "Current dy: $dy")

            Text(
                text = "Accumulated X: $totalX",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(text = "Accumulated Y: $totalY")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GestureTrackerPreview() {
    AndroidmouseTheme {
        GestureTracker()
    }
}