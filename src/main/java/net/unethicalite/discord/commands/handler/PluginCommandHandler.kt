package net.unethicalite.discord.commands.handler

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.unethicalite.dto.plugins.CreatePluginDto
import net.unethicalite.dto.plugins.PluginCreatedDto
import net.unethicalite.dto.plugins.PluginOverviewDto
import net.unethicalite.dto.plugins.UpdatePluginDto
import net.unethicalite.discord.commands.model.DiscordCommand
import net.unethicalite.dto.exception.BackendException
import net.unethicalite.discord.service.RestService
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class PluginCommandHandler(
    override val jda: JDA,
    private val restService: RestService
) : SlashCommandHandler() {
    override val commands = mapOf(
        "plugins" to DiscordCommand(
            name = "plugins",
            description = "List all available plugins.",
            reply = "Requesting plugins...",
            handler = ::showPlugins
        ),
        "plugin-info" to DiscordCommand(
            name = "plugin-info",
            description = "Show information about a specific plugin.",
            reply = "Requesting plugin...",
            options = mapOf(
                "plugin-id" to OptionData(
                    OptionType.INTEGER,
                    "plugin-id",
                    "The id of the plugin.",
                    true
                )
            ),
            handler = ::showPluginInfo,
        ),
        "plugin-create" to DiscordCommand(
            name = "plugin-create",
            description = "(Developers) Register a new plugin to an existing plan (requires approval).",
            reply = "Registering plugin...",
            private = true,
            options = mapOf(
                "jar-file" to OptionData(
                    OptionType.ATTACHMENT,
                    "jar-file",
                    "The plugin jar file. Will be automatically obfuscated once uploaded.",
                    true
                ),
                "name" to OptionData(
                    OptionType.STRING,
                    "name",
                    "The name of the plugin.",
                    true
                ),
                "main-class" to OptionData(
                    OptionType.STRING,
                    "main-class",
                    "The fully qualified name of the main class. (ex. dev.unethicalite.plugins.Plugin)",
                    true
                ),
                "description" to OptionData(
                    OptionType.STRING,
                    "description",
                    "A short description of the plugin.",
                    true
                ),
                "plan-id" to OptionData(
                    OptionType.INTEGER,
                    "plan-id",
                    "The id of the subscription plan to register the plugin to.",
                    true
                ),
                "image-url" to OptionData(
                    OptionType.STRING,
                    "image-url",
                    "(Optional) The url of the image to be displayed in the plugin list."
                ),
            ),
            handler = ::createPlugin,
        ),
        "plugin-update" to DiscordCommand(
            name = "plugin-update",
            description = "Update an existing plugin.",
            reply = "Updating plugin...",
            private = true,
            options = mapOf(
                "plugin-id" to OptionData(
                    OptionType.INTEGER,
                    "plugin-id",
                    "The id of the plugin.",
                    true
                ),
                "jar-file" to OptionData(
                    OptionType.ATTACHMENT,
                    "jar-file",
                    "The plugin jar file. Will be automatically obfuscated once uploaded."
                ),
                "name" to OptionData(
                    OptionType.STRING,
                    "name",
                    "The name of the plugin."
                ),
                "main-class" to OptionData(
                    OptionType.STRING,
                    "main-class",
                    "The fully qualified name of the main class. (ex. dev.unethicalite.plugins.Plugin)"
                ),
                "description" to OptionData(
                    OptionType.STRING,
                    "description",
                    "A short description of the plugin."
                ),
                "plan-id" to OptionData(
                    OptionType.INTEGER,
                    "plan-id",
                    "The id of the subscription plan to register the plugin to."
                ),
                "image-url" to OptionData(
                    OptionType.STRING,
                    "image-url",
                    "The url of the image to be displayed in the plugin list."
                ),
                "version" to OptionData(
                    OptionType.STRING,
                    "version",
                    "The new plugin version."
                ),
                "author" to OptionData(
                    OptionType.INTEGER,
                    "author",
                    "Transfer ownership to another developer (discord id)."
                )
            ),
            handler = ::updatePlugin,
        ),
        "plugin-delete" to DiscordCommand(
            name = "plugin-delete",
            description = "(Developers) Deletes a plugin.",
            reply = "Deleting plugin...",
            private = true,
            options = mapOf(
                "plugin-id" to OptionData(
                    OptionType.INTEGER,
                    "plugin-id",
                    "The id of the plugin to delete.",
                    true
                ),
            ),
            handler = ::deletePlugin,
        ),
        "plugin-buy" to DiscordCommand(
            name = "plugin-buy",
            description = "Purchase a plugin.",
            reply = "Purchasing plugin...",
            private = true,
            options = mapOf(
                "plugin-id" to OptionData(
                    OptionType.INTEGER,
                    "plugin-id",
                    "The id of the plugin to buy.",
                    true
                ),
            ),
            handler = ::buyPlugin,
        ),
    )

    fun showPlugins(event: SlashCommandInteractionEvent): EmbedBuilder {
        val builder = embedHelper.builder()
        builder.setTitle("Available plugins")
        builder.setColor(Color.MAGENTA)
        builder.setDescription("The following plugins are available. Use the /plugin-info <name> command to get more information.")

        val plugins = restService.get(
            "/plugins",
            Array<PluginOverviewDto>::class.java
        )

        plugins.forEach {
            builder.addField(
                "${it.name}#${it.id} - ${it.version}",
                "${it.description} (by ${jda.getUserById(it.author.discordId)?.asMention})",
                false
            )
        }

        return builder
    }

    fun showPluginInfo(event: SlashCommandInteractionEvent): EmbedBuilder {
        val id = event.getOption("plugin-id")?.asLong ?: throw BackendException("Invalid id.")
        val plugin = restService.get(
            "/plugins/$id",
            PluginOverviewDto::class.java
        )
        val builder = embedHelper.builder()
        val author = jda.getUserById(plugin.author.discordId)
        builder.setTitle("${plugin.name}#${plugin.id}")
        builder.setDescription(plugin.description)
        builder.setColor(Color.MAGENTA)
        builder.setThumbnail(plugin.imageUrl)
        builder.addField("Version", plugin.version, false)
        builder.addField("Uploaded", plugin.uploadDate.toLocaleString(), false)
        builder.addField("Last updated", plugin.updateDate.toLocaleString(), false)
        builder.addField("Subscription plan", "${plugin.plan.name}#${plugin.plan.id}", false)
        builder.addField(
            "Subscription price",
            "${plugin.plan.price} / ${plugin.plan.subscriptionPeriod}",
            false
        )
        builder.addField("Developer", "${author?.asMention}", false)
        return builder
    }

    fun createPlugin(event: SlashCommandInteractionEvent): String {
        val file = event.getOption("jar-file")?.asAttachment ?: throw BackendException("Invalid jar file.")
        if (file.fileExtension != "jar") {
            throw BackendException("Invalid file extension.")
        }

        val dto = CreatePluginDto(
            fileUrl = file.url,
            fileName = file.fileName,
            fileSize = file.size,
            name = event.getOption("name")?.asString ?: throw BackendException("Invalid name."),
            mainClass = event.getOption("main-class")?.asString ?: throw BackendException("Invalid main class."),
            description = event.getOption("description")?.asString
                ?: throw BackendException("Invalid description."),
            imageUrl = event.getOption("image-url")?.asString,
            planId = event.getOption("plan-id")?.asLong
                ?: throw BackendException("Invalid plan id.")
        )
        val plugin = restService.post("/plugins?discordId=${event.user.id}", dto, PluginCreatedDto::class.java)
        return "Plugin ${plugin.name}#${plugin.id} created successfully."
    }

    fun deletePlugin(event: SlashCommandInteractionEvent): String {
        val id = event.getOption("plugin-id")?.asLong ?: throw BackendException("Invalid id.")
        restService.delete("/plugins/$id?discordId=${event.user.id}")
        return "Plugin [$id] deleted successfully."
    }

    fun buyPlugin(event: SlashCommandInteractionEvent): String {
        val id = event.getOption("plugin-id")?.asLong ?: throw BackendException("Invalid id.")
        val plugin = restService.get("/plugins/$id/buy", PluginOverviewDto::class.java)
        return "This plugin belongs to plan: ${plugin.plan.name}#${plugin.plan.id}. " +
                "Use /subscribe <id> to purchase access to this plugin."
    }

    fun updatePlugin(event: SlashCommandInteractionEvent): String {
        val file = event.getOption("jar-file")?.asAttachment
        val dto = UpdatePluginDto(
            id = event.getOption("plugin-id")?.asLong ?: throw BackendException("Enter plugin id."),
            fileUrl = file?.url,
            fileName = file?.fileName,
            fileSize = file?.size,
            name = event.getOption("name")?.asString,
            mainClass = event.getOption("main-class")?.asString,
            description = event.getOption("description")?.asString,
            imageUrl = event.getOption("image-url")?.asString,
            planId = event.getOption("plan-id")?.asLong,
            author = event.getOption("author")?.asLong,
            version = event.getOption("version")?.asString
        )
        restService.put("/plugins?discordId=${event.user.id}", dto)
        return "Plugin ${dto.name}#${dto.id} updated successfully."
    }
}