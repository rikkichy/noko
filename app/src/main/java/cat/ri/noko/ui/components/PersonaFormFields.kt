package cat.ri.noko.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.R
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.model.PersonaType
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

@Composable
fun PersonaFormFields(
    type: PersonaType,
    avatarFileName: String?,
    onAvatarClick: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    nameError: Boolean = false,
    nameShakeOffset: Float = 0f,
    description: String,
    onDescriptionChange: (String) -> Unit,
    descError: Boolean = false,
    descShakeOffset: Float = 0f,
    personality: String = "",
    onPersonalityChange: (String) -> Unit = {},
    scenario: String = "",
    onScenarioChange: (String) -> Unit = {},
    showPersonalityScenario: Boolean = false,
    greetingMessage: String = "",
    onGreetingChange: (String) -> Unit = {},
    avatarSize: Dp = 128.dp,
    fallbackIcon: ImageVector = Icons.Filled.Person,
    namePlaceholder: String? = null,
    descriptionPlaceholder: String? = null,
) {
    val resolvedNamePlaceholder = namePlaceholder ?: stringResource(
        if (type == PersonaType.PERSONA) R.string.form_name_placeholder_persona
        else R.string.form_name_placeholder_character
    )
    val resolvedDescPlaceholder = descriptionPlaceholder ?: stringResource(
        if (type == PersonaType.PERSONA) R.string.form_description_placeholder_persona_default
        else R.string.form_description_placeholder_character_default
    )
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarFileName != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(AvatarStorage.getFile(context, avatarFileName))
                        .build(),
                    contentDescription = stringResource(R.string.form_avatar_content_desc),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    fallbackIcon,
                    contentDescription = stringResource(R.string.form_add_avatar_content_desc),
                    modifier = Modifier.size(avatarSize * 0.4f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.form_avatar_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(4.dp))

    OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= 100) onNameChange(it) },
        label = { Text(stringResource(R.string.form_name)) },
        placeholder = { Text(resolvedNamePlaceholder) },
        singleLine = true,
        isError = nameError,
        supportingText = if (nameError) {
            { Text(stringResource(R.string.form_name_required)) }
        } else null,
        shape = NokoFieldShape,
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(nameShakeOffset.toInt(), 0) },
    )

    OutlinedTextField(
        value = description,
        onValueChange = { if (it.length <= 10_000) onDescriptionChange(it) },
        label = { Text(stringResource(R.string.form_description)) },
        placeholder = { Text(resolvedDescPlaceholder) },
        isError = descError,
        supportingText = if (descError) {
            { Text(stringResource(R.string.form_description_required)) }
        } else null,
        shape = NokoFieldShape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .offset { IntOffset(descShakeOffset.toInt(), 0) },
    )

    if (type == PersonaType.CHARACTER) {
        if (showPersonalityScenario) {
            OutlinedTextField(
                value = personality,
                onValueChange = { if (it.length <= 5_000) onPersonalityChange(it) },
                label = { Text(stringResource(R.string.form_personality)) },
                placeholder = { Text(stringResource(R.string.form_personality_placeholder)) },
                shape = NokoFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )

            OutlinedTextField(
                value = scenario,
                onValueChange = { if (it.length <= 5_000) onScenarioChange(it) },
                label = { Text(stringResource(R.string.form_scenario)) },
                placeholder = { Text(stringResource(R.string.form_scenario_placeholder)) },
                shape = NokoFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )
        }

        OutlinedTextField(
            value = greetingMessage,
            onValueChange = { if (it.length <= 5_000) onGreetingChange(it) },
            label = { Text(stringResource(R.string.form_greeting)) },
            placeholder = { Text(stringResource(R.string.form_greeting_placeholder)) },
            shape = NokoFieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
        )
    }
}
