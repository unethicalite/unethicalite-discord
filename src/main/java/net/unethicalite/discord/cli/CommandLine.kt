package net.unethicalite.discord.cli

import net.dv8tion.jda.api.JDA
import net.unethicalite.discord.commands.handler.SlashCommandHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CommandLine(
    private val context: ApplicationContext,
    private val jda: JDA,
    @Value("\${discord.bot.guild-id}")
    private val guildId: String
) {
    private val logger = LoggerFactory.getLogger(SlashCommandHandler::class.java)

    @Scheduled(fixedRate = 500)
    fun loop() {
        val read = readLine() ?: return
        val input = read.split(" ")
        if (input.size < 2) {
            return
        }

        val guild = jda.getGuildById(guildId) ?: return
        val command = input[0]
        val action = input[1]
        if (command == "commands") {
            when (action) {
                "get" -> {
                    logger.info("${guild.retrieveCommands().complete().map { it.name }}")
                }

                "delete" -> {
                    if (input.size < 3) return
                    val sub = input[2]
                    if (sub == "all") {
                        guild.retrieveCommands().complete().forEach { it?.delete()?.queue() }
                    } else {
                        guild.retrieveCommands().complete().firstOrNull { it.name == sub }?.delete()?.queue()
                    }
                }

                "register" -> {
                    if (input.size < 3) return
                    val sub = input[2]
                    val bean = context.getBean(sub)
                    if (bean is SlashCommandHandler) {
                        bean.registerCommands()
                    }
                }
            }
        }
    }
}