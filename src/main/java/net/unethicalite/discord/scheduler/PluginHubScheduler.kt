package net.unethicalite.discord.scheduler

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.unethicalite.discord.config.properties.DiscordProperties
import net.unethicalite.discord.helpers.EmbedHelper
import net.unethicalite.discord.service.RestService
import net.unethicalite.dto.github.PluginRepoDto
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PluginHubScheduler(
    private val jda: JDA,
    private val guild: Guild,
    private val restService: RestService,
    private val discordProperties: DiscordProperties,
    private val embedHelper: EmbedHelper
) {
    @Scheduled(cron = "\${discord.bot.plugin-hub-cron}")
    private fun postMissingRepos() {
        val repos = restService.get("/repos", Array<PluginRepoDto>::class.java)
            .filter { it.messageId.isNullOrEmpty() }

        for (repo in repos) {
            val user = guild.getMemberById(repo.ownerId) ?: continue
            val messageId = sendMessage(user, repo)?.id ?: continue
            restService.put("/repos/${repo.repoId}?messageId=$messageId&ownerId=${repo.ownerId}", null)
        }
    }

    @Scheduled(cron = "\${discord.bot.plugin-hub-cron}")
    private fun updateRepos() {
        val repos = restService.get("/repos", Array<PluginRepoDto>::class.java)
            .filter { !it.messageId.isNullOrEmpty() }
        for (repo in repos) {
            val user = guild.getMemberById(repo.ownerId) ?: continue
            jda.getTextChannelById(discordProperties.pluginHubChannelId)
                ?.editMessageEmbedsById(
                    repo.messageId!!,
                    embedHelper.builder("Unethicalite Hub")
                        .setImage(user.avatarUrl)
                        .addField("Owner", user.user.asMention, false)
                        .addField("URL", repo.repoUrl, false)
                        .addField("Plugins", repo.plugins.size.toString(), false)
                        .build()
                )
                ?.queue(null) {
                    restService.put("/repos/${repo.repoId}?messageId=&ownerId=${repo.ownerId}", null)
                }
        }
    }

    private fun sendMessage(user: Member, repo: PluginRepoDto): Message? {
        return jda.getTextChannelById(discordProperties.pluginHubChannelId)
            ?.sendMessageEmbeds(embedHelper.builder("Unethicalite Hub")
                .setImage(user.avatarUrl)
                .addField("Owner", user.user.asMention, false)
                .addField("URL", repo.repoUrl, false)
                .addField("Plugins", repo.plugins.size.toString(), false)
                .build())
            ?.complete()
    }
}