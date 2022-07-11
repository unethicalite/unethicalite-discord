package net.unethicalite.discord.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.unethicalite.discord.config.properties.DiscordProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DiscordProperties::class)
class DiscordConfig(
    private val discordProperties: DiscordProperties
) {
    @Bean
    fun jda() = JDABuilder
        .createDefault(discordProperties.token)
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .build()
        .awaitReady()

    @Bean
    fun guild(jda: JDA) = jda.getGuildById(discordProperties.guildId)?.also {
        it.loadMembers()
    } ?: throw RuntimeException("Guild not found.")
}