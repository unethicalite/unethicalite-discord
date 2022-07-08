package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.unethicalite.discord.commands.model.CommandResponse
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.discord.helpers.EmbedHelper
import net.unethicalite.dto.exception.BackendException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.awt.Color
import java.net.MalformedURLException
import java.net.URL
import javax.annotation.PostConstruct

abstract class SlashCommandHandler : ListenerAdapter() {
    @Value("\${discord.bot.guild-id}")
    private lateinit var guildId: String

    @Autowired
    protected lateinit var embedHelper: EmbedHelper
    private val logger = LoggerFactory.getLogger(SlashCommandHandler::class.java)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        val command = commands[commandName] ?: return
        val sender = event.user

        logger.info("Command '${event.name}' received from ${sender.name}, ${event.options}")
        var embed = embedHelper.builder()

        try {
            val start = System.currentTimeMillis()
            when (val response = command.handler(event)) {
                is String -> {
                    embed
                        .setTitle("Unethicalite")

                    if (response.isImage()) {
                            embed.setImage(response)
                    } else {
                        embed.setDescription(response)
                    }
                }

                is CommandResponse -> {
                    embed
                        .setTitle(response.title)
                        .setDescription(response.body)
                }

                is EmbedBuilder -> {
                    embed = response
                }
            }
            embed.setFooter("Response time: ${System.currentTimeMillis() - start} ms")
        } catch (ex: BackendException) {
            embed.setTitle("Error")
            embed.setColor(Color.RED)
            embed.setDescription(ex.message)
        } catch (ex: Exception) {
            logger.error("Command execution failed.", ex)
            embed.setTitle("Error")
            embed.setColor(Color.RED)
            embed.setDescription("Failed to execute command.")
        } finally {
            val msg = embed.build()
            event
                .replyEmbeds(msg)
                .setEphemeral(command.private)
                .queue()
        }
    }

    @PostConstruct
    fun registerListener() {
        jda.addEventListener(this)
    }

    fun registerCommands() {
        val commandData = mutableListOf<CommandData>()
        commands.forEach { (_, command) ->
            commandData.add(command.buildSlashCommand())
        }

        val guild = jda.getGuildById(guildId)
        if (guild == null) {
            logger.error("Could not find guild with id $guildId")
            return
        }

        commandData.forEach {
            logger.info("Registering command '${it.name}'")
            guild.upsertCommand(it).complete()
        }
    }

    protected abstract val jda: JDA
    protected abstract val commands: Map<String, DiscordCommand<*>>

    fun String.isImage() = try {
        URL(this)
        true
    } catch (e: MalformedURLException) {
        false
    }
}