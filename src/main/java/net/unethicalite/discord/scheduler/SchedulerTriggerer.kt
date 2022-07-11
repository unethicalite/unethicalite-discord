package net.unethicalite.discord.scheduler

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class SchedulerTriggerer(
    private val pluginHubScheduler: PluginHubScheduler,
    private val statusScheduler: StatusScheduler
) {
    fun triggerPluginHubScheduler() {
        pluginHubScheduler.postMissingRepos()
        pluginHubScheduler.updateRepos()
    }

    fun triggerStatusScheduler() {
        statusScheduler.updateBotStatus()
    }

    @PostConstruct
    fun init() {
        triggerPluginHubScheduler()
        triggerStatusScheduler()
    }
}