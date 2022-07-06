package net.unethicalite.discord.scheduler

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.unethicalite.dto.exception.BackendException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class StatusScheduler(
    private val jda: JDA,
    private val restTemplate: RestTemplate
) {
    @Scheduled(cron = "0 */5 * * * *")
    private fun updateBotStatus() {
        jda.presence.activity = Activity.playing("" +
                "${restTemplate.getForObject("http://localhost:8080/sessions", Int::class.java) 
                    ?: throw BackendException("Request failed.")} users online."
        )
    }
}