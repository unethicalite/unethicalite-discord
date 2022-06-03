package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.unethicalite.dto.github.GitHubIssueDto
import net.unethicalite.discord.commands.model.CommandResponse
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.dto.exception.BackendException
import net.unethicalite.dto.exception.NotFoundException
import net.unethicalite.discord.messaging.RestService
import org.springframework.stereotype.Component

@Component
class GitHubCommandHandler(
    override val jda: JDA,
    private val restService: RestService
) : SlashCommandHandler() {
    override val commands = mapOf(
        "report-bug" to DiscordCommand(
            name = "report-bug",
            description = "Submit a bug report on GitHub.",
            private = true,
            options = mapOf(
                "title" to OptionData(OptionType.STRING, "title", "The title of the bug report.", true),
                "description" to OptionData(OptionType.STRING, "description", "A clear description of the bug.", true),
            ),
            handler = ::createIssue,
            extra = "Bug"
        ),
        "create-suggestion" to DiscordCommand(
            name = "create-suggestion",
            description = "Create a suggestion on GitHub.",
            private = true,
            options = mapOf(
                "title" to OptionData(OptionType.STRING, "title", "The title of the suggestion.", true),
                "description" to OptionData(OptionType.STRING, "description", "A description of the suggestion.", true),
            ),
            handler = ::createIssue,
            extra = "Suggestion"
        )
    )

    fun createIssue(event: SlashCommandInteractionEvent): CommandResponse {
        val title = event.getOption("title")?.asString ?: throw BackendException("Invalid title.")
        val desc = event.getOption("description")?.asString ?: throw BackendException("Invalid description.")
        val type = commands[event.commandPath]?.extra ?: throw NotFoundException("Command not found.")
        val body = """
            $type submitted by: ${event.user.asTag} (${event.user.id})

            $desc
        """.trimIndent()

        return CommandResponse(
            "GitHub",
            "Issue successfully created at: " + restService.post("/github/issues", GitHubIssueDto(
                type.lowercase(),
                title,
                body
            ), String::class.java)
        )
    }
}