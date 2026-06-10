package ru.akarakuts.russiancheckers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RulesSection(R.string.rules_section_start, R.string.rules_body_start)
        RulesSection(R.string.rules_section_moves, R.string.rules_body_moves)
        RulesSection(R.string.rules_section_captures, R.string.rules_body_captures)
        RulesSection(R.string.rules_section_kings, R.string.rules_body_kings)
        RulesSection(R.string.rules_section_win, R.string.rules_body_win)
    }
}

@Composable
private fun RulesSection(titleId: Int, bodyId: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(titleId), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(bodyId), style = MaterialTheme.typography.bodyLarge)
    }
}
