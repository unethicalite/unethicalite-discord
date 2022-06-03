package net.unethicalite.discord.config

import net.dv8tion.jda.api.JDABuilder
import net.unethicalite.discord.config.properties.DiscordProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DiscordProperties::class)
class DiscordConfig(
    @Value("\${discord.bot.token}")
    private val botToken: String
) {
    @Bean
    fun jda() = JDABuilder.createDefault(botToken).build().awaitReady()
}