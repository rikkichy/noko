package cat.ri.noko.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import cat.ri.noko.model.builtInProviders
import cat.ri.noko.model.legacyProviders
import cat.ri.noko.ui.util.rememberNokoHaptics

@Composable
fun ProviderListContent(
    selectedProviderId: String,
    onProviderSelect: (String) -> Unit,
) {
    val haptics = rememberNokoHaptics()

    CustomProviderCard(
        isSelected = selectedProviderId == "custom",
        onSelect = {
            haptics.tap()
            onProviderSelect("custom")
        },
    )

    builtInProviders.forEach { provider ->
        ProviderCard(
            provider = provider,
            isSelected = selectedProviderId == provider.id,
            onSelect = {
                haptics.tap()
                onProviderSelect(provider.id)
            },
        )
    }

    Text(
        "Legacy",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    legacyProviders.forEach { provider ->
        ProviderCard(
            provider = provider,
            isSelected = selectedProviderId == provider.id,
            onSelect = {
                haptics.tap()
                onProviderSelect(provider.id)
            },
        )
    }
}
