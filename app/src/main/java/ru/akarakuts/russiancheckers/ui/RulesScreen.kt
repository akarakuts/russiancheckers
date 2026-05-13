package ru.akarakuts.russiancheckers.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.akarakuts.russiancheckers.R

/** Scrollable rules text from string resources. */
@Composable
fun RulesScreen(modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Text(
        text = stringResource(R.string.rules_body),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(scroll),
    )
}
