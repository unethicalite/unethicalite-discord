package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.discord.scheduler.SchedulerTriggerer
import net.unethicalite.discord.service.RestService
import net.unethicalite.dto.exception.BackendException
import org.springframework.stereotype.Component

@Component
class PluginHubCommandHandler(
    override val jda: JDA,
    private val restService: RestService,
    private val schedulerTriggerer: SchedulerTriggerer
) : SlashCommandHandler() {
    override val commands = mapOf(
        "hub-addrepo" to DiscordCommand(
            "hub-addrepo",
            description = "Registers your plugin repo on the unethicalite plugin hub.",
            handler = ::addRepo,
            options = mapOf(
                "github-repo-id" to OptionData(
                    OptionType.STRING,
                    "github-repo-id",
                    "Github Repository ID.",
                    true
                )
            )
        )
    )

    fun addRepo(e: SlashCommandInteractionEvent): String {
        val repoId = e.getOption("github-repo-id")?.asString
            ?: throw BackendException("No repository id entered.")
        restService.post("/repos?repoId=$repoId&ownerId=${e.user.id}", null, Int::class.java)
        schedulerTriggerer.triggerPluginHubScheduler()
        return "Successfully registered repo."
    }
}