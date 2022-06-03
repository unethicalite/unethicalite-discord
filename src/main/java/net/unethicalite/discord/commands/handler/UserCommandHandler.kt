package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.unethicalite.dto.users.UserDto
import net.unethicalite.dto.users.UserRegisteredDto
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.dto.exception.BackendException
import net.unethicalite.discord.messaging.RestService
import org.springframework.stereotype.Service
import java.awt.Color
import java.util.*

@Service
class UserCommandHandler(
    override val jda: JDA,
    private val restService: RestService
) : SlashCommandHandler() {
    override val commands = mapOf(
        "register" to DiscordCommand(
            name = "register",
            description = "Registers you and creates an api key.",
            private = true,
            reply = "Registering...",
            handler = ::registerUser,
            options = mapOf(
                "type" to OptionData(OptionType.STRING, "type", "Type of user group to register you as.", true)
                    .addChoice("customer", "CUSTOMER")
                    .addChoice("developer", "DEVELOPER")
            )
        ),
        "me" to DiscordCommand(
            name = "me",
            description = "Displays user information (such as current subscriptions, balance, etc.)",
            private = true,
            reply = "Requesting user info...",
            handler = ::showInfo
        )
    )

    fun registerUser(event: SlashCommandInteractionEvent): String {
        val type = event.getOption("type")?.asString ?: throw BackendException("Invalid type.")
        val result = restService.post(
            "/users?name=${event.user.name}&discordId=${event.user.id}&type=$type",
            null,
            UserRegisteredDto::class.java
        )

        return "Successfully registered as **${result.role}**. Your api key is: ${result.apikey} " +
                "Developer registrations require approval from the admins."
    }

    fun showInfo(event: SlashCommandInteractionEvent): EmbedBuilder {
        val user = restService.get("/users/${event.user.id}", UserDto::class.java)
        val builder = embedHelper.builder()
        builder.setTitle("User info")
        builder.setDescription(
            "${jda.getUserById(event.user.id)?.asMention}, your API key is: ${user.apiKey}.\n" +
                    "Your balance is ${user.balance} credits.\n" +
                    "You are subscribed to ${user.subscriptions.size} plans."
        )
        builder.setColor(Color.YELLOW)
        builder.setThumbnail(event.user.avatarUrl)
        builder.setFooter("Registered since ${user.registrationDate}.")

        user.subscriptions.forEach {
            builder.addField(
                "Subscription: ${it.planName}#${it.planId}",
                "Expires: ${Date.from(it.expires)}",
                false
            )
        }

        return builder
    }
}