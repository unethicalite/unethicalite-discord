package net.unethicalite.discord.scheduler

import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class SchedulerTriggerer(
    private val pluginHubScheduler: PluginHubScheduler,
    private val statusScheduler: StatusScheduler
) {
    private val executorService = Executors.newScheduledThreadPool(5)

    fun triggerPluginHubScheduler() {
        executorService.schedule({ pluginHubScheduler.postMissingRepos() }, 0, TimeUnit.SECONDS)
        executorService.schedule({ pluginHubScheduler.updateRepos() }, 5, TimeUnit.SECONDS)
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