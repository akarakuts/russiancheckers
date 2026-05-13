package ru.akarakuts.russiancheckers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.akarakuts.russiancheckers.R
import ru.akarakuts.russiancheckers.game.AiDifficulty

/** Bot, colour, AI strength, and new-game actions. */
@Composable
fun SettingsScreen(vm: CheckersViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_section_game),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_vs_bot),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_vs_bot_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.botEnabled,
                onCheckedChange = vm::setBotEnabled,
            )
        }
        if (state.botEnabled) {
            Text(
                text = stringResource(R.string.settings_human_color),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.humanIsWhite,
                    onClick = { vm.setHumanIsWhite(true) },
                    label = { Text(stringResource(R.string.play_as_white)) },
                )
                FilterChip(
                    selected = !state.humanIsWhite,
                    onClick = { vm.setHumanIsWhite(false) },
                    label = { Text(stringResource(R.string.play_as_black)) },
                )
            }
            Text(
                text = stringResource(R.string.settings_bot_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_ai_difficulty),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_ai_difficulty_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Easy,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Easy) },
                    label = { Text(stringResource(R.string.difficulty_easy)) },
                )
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Normal,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Normal) },
                    label = { Text(stringResource(R.string.difficulty_normal)) },
                )
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Hard,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Hard) },
                    label = { Text(stringResource(R.string.difficulty_hard)) },
                )
            }
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.settings_section_data),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(
            onClick = { vm.newGame() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_new_party_clear))
        }
        Spacer(Modifier.height(8.dp))
    }
}
