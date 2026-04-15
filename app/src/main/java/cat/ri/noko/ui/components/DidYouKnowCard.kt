package cat.ri.noko.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.PersonaEntry
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

private val triviaFacts = listOf(
    "Internet RP appeared in the early 1990s, where people used to chat with each other. Things have changed.. yeah.",
    "If your response is long and structured, AI is less prone to impersonate you.",
    "Adding a detailed persona description helps the AI understand your writing style.",
    "Using *action blocks* in your messages helps AI distinguish narration from dialogue.",
    "NokoGuard detects AI hallucinations really well.",
    "Secret chats aren't saved to history. Just saying..",
    "Shorter system prompts often lead to more creative AI responses.",
    "The term 'roleplay' in online contexts dates back to IRC channels in the early 90s.",
)

private sealed class CardItem {
    data class Fact(val text: String) : CardItem()
    data class CharacterStat(val name: String, val count: Int, val avatarFileName: String?) : CardItem()
    data class PersonaStat(val name: String, val messageCount: Int, val avatarFileName: String?) : CardItem()
}

@Composable
fun DidYouKnowCard(
    recentChats: List<ChatSessionMeta>,
    entryMap: Map<String, PersonaEntry>,
    refreshKey: Int = 0,
) {
    val titles = listOf("Did you know?", "Interesting fact..", "Fun fact!", "By the way..", "Hmm..")

    val items = remember(recentChats, entryMap, refreshKey) {
        val pool = mutableListOf<CardItem>()
        pool.addAll(triviaFacts.map { CardItem.Fact(it) })

        val charGroups = recentChats
            .filter { it.messageCount > 0 }
            .groupBy { it.characterId }
        for ((charId, chats) in charGroups) {
            val name = chats.first().characterName
            val avatar = entryMap[charId]?.avatarFileName
                ?: chats.firstOrNull { it.characterAvatarFileName != null }?.characterAvatarFileName
            pool.add(CardItem.CharacterStat(name, chats.size, avatar))
        }

        val personaGroups = recentChats
            .filter { it.personaName != null && it.messageCount > 0 }
            .groupBy { it.personaName!! }
        for ((name, chats) in personaGroups) {
            val personaEntry = entryMap.values.find { it.name == name }
            val avatar = personaEntry?.avatarFileName
                ?: chats.firstOrNull { it.personaAvatarFileName != null }?.personaAvatarFileName
            pool.add(CardItem.PersonaStat(name, chats.sumOf { it.messageCount }, avatar))
        }

        pool.random()
    }

    val title = remember(refreshKey) { titles.random() }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            when (items) {
                is CardItem.Fact -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            items.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is CardItem.CharacterStat -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (items.avatarFileName != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(AvatarStorage.getFile(context, items.avatarFileName))
                                    .build(),
                                contentDescription = items.name,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "You have ${items.count} chat${if (items.count != 1) "s" else ""} with ${items.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is CardItem.PersonaStat -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (items.avatarFileName != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(AvatarStorage.getFile(context, items.avatarFileName))
                                    .build(),
                                contentDescription = items.name,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "You've sent ${items.messageCount} message${if (items.messageCount != 1) "s" else ""} as ${items.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
