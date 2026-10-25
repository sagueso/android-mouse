package ve.com.sagues.rafael.android_mouse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ve.com.sagues.rafael.android_mouse.HostKeyboardLayout
import ve.com.sagues.rafael.android_mouse.ui.theme.Mint
import ve.com.sagues.rafael.android_mouse.ui.theme.TextMuted

/**
 * Compact selector for the host computer's keyboard layout. It is used both from
 * the remote keyboard dialog and from the trackpad preferences.
 */
@Composable
fun KeyboardLayoutSelector(
    selected: HostKeyboardLayout,
    onSelect: (HostKeyboardLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(34.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(start = 10.dp, end = 4.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Text(selected.label, fontSize = 11.sp)
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Host keyboard layout",
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HostKeyboardLayout.entries.forEach { layout ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(layout.label, fontSize = 13.sp)
                            Text(layout.hint, fontSize = 10.sp, color = TextMuted)
                        }
                    },
                    onClick = {
                        onSelect(layout)
                        expanded = false
                    },
                    leadingIcon = if (selected == layout) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = Mint) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
