package ru.akarakuts.russiancheckers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.akarakuts.russiancheckers.R
import ru.akarakuts.russiancheckers.game.AiDifficulty

/** Bot, colour, AI strength, coordinates, and new-game actions. */
@Composable
fun SettingsScreen(
    vm: CheckersViewModel,
    onNewGameRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_section_game),
            style = MaterialTheme.typography.titleMedium,
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_vs_bot),
            subtitle = stringResource(R.string.settings_vs_bot_sub),
            checked = state.botEnabled,
            onCheckedChange = vm::setBotEnabled,
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_show_coordinates),
            subtitle = stringResource(R.string.settings_show_coordinates_sub),
            checked = state.showCoordinates,
            onCheckedChange = vm::setShowCoordinates,
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_sound),
            subtitle = stringResource(R.string.settings_sound_sub),
            checked = state.soundEnabled,
            onCheckedChange = vm::setSoundEnabled,
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_haptics),
            subtitle = stringResource(R.string.settings_haptics_sub),
            checked = state.hapticsEnabled,
            onCheckedChange = vm::setHapticsEnabled,
        )
        if (state.botEnabled) {
            Text(
                text = stringResource(R.string.settings_human_color),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.humanIsWhite,
                    onClick = { vm.setHumanIsWhite(true) },
                    label = { Text(stringResource(R.string.play_as_white)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
                FilterChip(
                    selected = !state.humanIsWhite,
                    onClick = { vm.setHumanIsWhite(false) },
                    label = { Text(stringResource(R.string.play_as_black)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Easy,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Easy) },
                    label = { Text(stringResource(R.string.difficulty_easy)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Normal,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Normal) },
                    label = { Text(stringResource(R.string.difficulty_normal)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Hard,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Hard) },
                    label = { Text(stringResource(R.string.difficulty_hard)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
                FilterChip(
                    selected = state.aiDifficulty == AiDifficulty.Expert,
                    onClick = { vm.setAiDifficulty(AiDifficulty.Expert) },
                    label = { Text(stringResource(R.string.difficulty_expert)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
            }
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.settings_section_stats),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.stats.total == 0) {
            Text(
                text = stringResource(R.string.stats_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            StatsRow(stringResource(R.string.stats_wins), state.stats.wins.toString())
            StatsRow(stringResource(R.string.stats_losses), state.stats.losses.toString())
            StatsRow(stringResource(R.string.stats_streak), state.stats.winStreak.toString())
            StatsRow(stringResource(R.string.stats_best_streak), state.stats.bestStreak.toString())
            OutlinedButton(
                onClick = vm::resetStats,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.stats_reset))
            }
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.settings_section_data),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(
            onClick = onNewGameRequest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_new_party_clear))
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
