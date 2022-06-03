package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.unethicalite.dto.plans.CreatePlanDto
import net.unethicalite.dto.plans.PlanCreatedDto
import net.unethicalite.dto.plans.PlanOverviewDto
import net.unethicalite.dto.plans.SubscribedDto
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.dto.exception.BackendException
import net.unethicalite.discord.service.RestService
import org.springframework.stereotype.Service
import java.awt.Color
import java.util.*

@Service
class PlanCommandHandler(
    override val jda: JDA,
    private val restService: RestService
) : SlashCommandHandler() {
    override val commands = mapOf(
        "plans" to DiscordCommand(
            name = "plans",
            description = "Shows all available subscription plans.",
            reply = "Requesting plans...",
            handler = ::showPlans
        ),
        "plan-info" to DiscordCommand(
            name = "plan-info",
            description = "Shows information for a specific subscription.",
            reply = "Requesting plan...",
            handler = ::showInfo,
            options = mapOf(
                "id" to OptionData(
                    OptionType.STRING,
                    "id",
                    "The ID of the subscription plan to show information for.",
                    true
                )
            )
        ),
        "subscribe" to DiscordCommand(
            name = "subscribe",
            description = "Subscribes to a plan.",
            reply = "Subscribing...",
            private = true,
            options = mapOf(
                "id" to OptionData(
                    OptionType.INTEGER,
                    "id",
                    "The ID of the subscription plan to purchase.",
                    true
                )
            ),
            handler = ::subscribe
        ),
        "plan-create" to DiscordCommand(
            name = "plan-create",
            description = "(Developers) Creates a new subscription plan.",
            reply = "Creating plan...",
            private = true,
            options = mapOf(
                "name" to OptionData(
                    OptionType.STRING,
                    "name",
                    "The name of the subscription plan.",
                    true
                ),
                "description" to OptionData(
                    OptionType.STRING,
                    "description",
                    "The description of the subscription plan.",
                    true
                ),
                "price" to OptionData(
                    OptionType.INTEGER,
                    "price",
                    "The price (in credits) of the subscription plan.",
                    true
                ),
                "period" to OptionData(
                    OptionType.STRING,
                    "period",
                    "The subscription period.",
                    true
                ).addChoice("daily", "DAILY")
                    .addChoice("weekly", "WEEKLY")
                    .addChoice("monthly", "MONTHLY")
                    .addChoice("yearly", "YEARLY")
                    .addChoice("lifetime", "LIFETIME"),
                "image-url" to OptionData(
                    OptionType.STRING,
                    "image-url",
                    "(Optional) The plan logo/image."
                ),
            ),
            handler = ::create
        ),
        "plan-delete" to DiscordCommand(
            name = "plan-delete",
            description = "(Developers) Deletes a subscription plan.",
            reply = "Deleting plan...",
            private = true,
            options = mapOf(
                "id" to OptionData(
                    OptionType.INTEGER,
                    "id",
                    "The ID of the subscription plan to delete.",
                    true
                )
            ),
            handler = ::delete
        )
    )

    fun showPlans(event: SlashCommandInteractionEvent): EmbedBuilder {
        val builder = embedHelper.builder()
        builder.setTitle("Available subscription plans")
        builder.setColor(Color.CYAN)
        builder.setDescription("The following subscription plans are available. Use the /plan-info <id> command to get more information.")

        restService.get("/plans", Array<PlanOverviewDto>::class.java).forEach {
            builder.addField(
                "${it.name}#${it.id}",
                "${it.description} (by ${jda.getUserById(it.owner.discordId)?.asMention})",
                false
            )
        }

        return builder
    }

    fun subscribe(event: SlashCommandInteractionEvent): String {
        val id = event.getOption("id")?.asLong ?: throw BackendException("Invalid id.")
        val subscription = restService.get(
            "/plans/$id/subscribe?discordId=${event.user.id}",
            SubscribedDto::class.java
        )
        return "Successfully subscribed.\n" +
                "Your final balance is **${subscription.finalBalance} credits**.\n" +
                "The subscription will expire on **${Date.from(subscription.expirationDate)}**."
    }

    fun create(event: SlashCommandInteractionEvent): String {
        val plan = restService.post(
            "/plans?discordId=${event.user.id}",
            CreatePlanDto(
                name = event.getOption("name")?.asString
                    ?: throw BackendException("Invalid name."),
                description = event.getOption("description")?.asString
                    ?: throw BackendException("Invalid description."),
                imageUrl = event.getOption("image-url")?.asString,
                subscriptionPeriod = event.getOption("period")?.asString?.uppercase()
                    ?: throw BackendException("Invalid subscription period."),
                price = event.getOption("price")?.asInt
                    ?: throw BackendException("Invalid price.")
            ),
            PlanCreatedDto::class.java
        )

        return "Plan ${plan.name}#${plan.id} successfully created."
    }

    fun showInfo(event: SlashCommandInteractionEvent): EmbedBuilder {
        val id = event.getOption("id")?.asLong ?: throw BackendException("Invalid id.")
        val sub = restService.get("/plans/$id", PlanOverviewDto::class.java)
        val builder = embedHelper.builder()
        val owner = jda.getUserById(sub.owner.discordId)
        builder.setTitle("${sub.name}#${sub.id}")
        builder.setDescription(sub.description)
        builder.setColor(Color.CYAN)
        builder.setThumbnail(sub.imageUrl)
        builder.addField("Period", sub.subscriptionPeriod, false)
        builder.addField("Price", sub.price.toString(), false)
        builder.addField("Owner", "${owner?.asMention}", false)
        return builder
    }

    fun delete(event: SlashCommandInteractionEvent): String {
        val id = event.getOption("id")?.asLong ?: throw BackendException("Invalid id.")
        restService.delete("/plans/$id?discordId=${event.user.id}")
        return "Plan deleted."
    }
}