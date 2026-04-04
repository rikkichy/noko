package cat.ri.noko.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.model.PersonaEntry
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

@Composable
fun NokoAvatar(
    entry: PersonaEntry?,
    fallbackIcon: ImageVector,
    size: Int,
    hasSurface: Boolean = true,
) {
    NokoAvatar(
        name = entry?.name,
        avatarFileName = entry?.avatarFileName,
        fallbackIcon = fallbackIcon,
        size = size,
        hasSurface = hasSurface,
    )
}

@Composable
fun NokoAvatar(
    name: String?,
    avatarFileName: String?,
    fallbackIcon: ImageVector,
    size: Int,
    hasSurface: Boolean = true,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarFileName != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AvatarStorage.getFile(context, avatarFileName))
                    .build(),
                contentDescription = name,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else if (hasSurface) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(size.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        fallbackIcon,
                        contentDescription = null,
                        modifier = Modifier.size((size / 2).dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Icon(
                fallbackIcon,
                contentDescription = null,
                modifier = Modifier.size((size / 2).dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
