package net.unethicalite.discord.scheduler

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.unethicalite.discord.config.properties.DiscordProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import javax.annotation.PostConstruct

@Component
class StatusScheduler(
    private val jda: JDA,
    private val restTemplate: RestTemplate,
    private val discordProperties: DiscordProperties
) {
    @Scheduled(cron = "\${discord.bot.status-cron}")
    private fun updateBotStatus() {
        println("Checking status")
        jda.presence.activity = Activity.playing("" +
                "${restTemplate.getForObject(discordProperties.sessionsUrl, Int::class.java) ?: 0} clients connected."
        )
    }

    @PostConstruct
    fun init() {
        updateBotStatus()
    }
}