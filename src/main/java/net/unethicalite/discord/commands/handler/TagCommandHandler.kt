package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.discord.service.RestService
import net.unethicalite.dto.discord.TagDto
import net.unethicalite.dto.exception.BackendException
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class TagCommandHandler(
    override val jda: JDA,
    private val restService: RestService
) : SlashCommandHandler() {
    override val commands = mapOf(
        "tag" to DiscordCommand(
            name = "tag",
            description = "Add, remove or show tags.",
            handler = ::tag,
            options = mapOf(
                "name" to OptionData(
                    OptionType.STRING,
                    "name",
                    "The name of the tag.",
                    true
                ),
                "operation" to OptionData(
                    OptionType.STRING,
                    "operation",
                    "Add or delete.",
                    false
                ).addChoice("add", "add")
                    .addChoice("delete", "delete"),
                "text" to OptionData(
                    OptionType.STRING,
                    "text",
                    "Text to attach to this tag.",
                    false
                )
            )
        ),
    )

    fun tag(e: SlashCommandInteractionEvent): EmbedBuilder {
        val name = e.getOption("name")?.asString ?: throw BackendException("Invalid tag name given.")
        val embed = embedHelper.builder()

        val text = when (e.getOption("operation")?.asString) {
            "add" -> {
                val text = e.getOption("text")?.asString
                    ?.replace("@", "'at'")
                    ?: throw BackendException("No text entered.")
                restService.post("/tags?name=$name&text=$text&ownerId=${e.user.id}", null, Any::class.java)
                "Tag added."
            }

            "delete" -> {
                restService.delete("/tags?name=$name&ownerId=${e.user.id}")
                "Tag deleted."
            }

            else -> {
                restService.get("/tags/$name", TagDto::class.java).text
            }
        }

        embed.setTitle(name)
        embed.setColor(Color.YELLOW)

        if (text.isImage()) {
            embed.setImage(text)
        } else {
            embed.setDescription(text)
        }

        return embed
    }
}