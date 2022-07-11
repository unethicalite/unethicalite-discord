package net.unethicalite.discord.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "discord.bot")
@ConstructorBinding
data class DiscordProperties(
    val token: String,
    val guildId: String,
    val statusCron: String,
    val sessionsUrl: String,
    val pluginHubChannelId: String,
    val pluginHubCron: String,
    val pluginHubSchedulerEnabled: Boolean
)