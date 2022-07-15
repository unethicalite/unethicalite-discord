package net.unethicalite.discord.scheduler

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.unethicalite.discord.config.properties.DiscordProperties
import net.unethicalite.discord.helpers.EmbedHelper
import net.unethicalite.discord.service.RestService
import net.unethicalite.dto.github.PluginRepoDto
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class PluginHubScheduler(
    private val jda: JDA,
    private val guild: Guild,
    private val restService: RestService,
    private val discordProperties: DiscordProperties,
    private val embedHelper: EmbedHelper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${discord.bot.plugin-hub-cron}")
    fun postMissingRepos() {
        if (!discordProperties.pluginHubSchedulerEnabled) return
        log.info("Going to post missing repos")
        val repos = restService.get("/repos", Array<PluginRepoDto>::class.java)
            .filter { it.messageId.isNullOrEmpty() }
        guild.loadMembers().onSuccess { members ->
            for (repo in repos) {
                val user = members.firstOrNull { it.user.id == repo.ownerId }
                if (user == null) {
                    println("${repo.ownerId} not found in user list ${guild.name}")
                    continue
                }

                val messageId = sendMessage(user, repo)?.id
                if (messageId == null) {
                    println("Message not sent")
                    continue
                }

                restService.put("/repos/${repo.repoId}?messageId=$messageId&ownerId=${repo.ownerId}", null)
            }
        }
    }

    @Scheduled(cron = "\${discord.bot.plugin-hub-cron}")
    fun updateRepos() {
        if (!discordProperties.pluginHubSchedulerEnabled) return
        log.info("Going to update repos")
        val repos = restService.get("/repos", Array<PluginRepoDto>::class.java)
            .filter { !it.messageId.isNullOrEmpty() }
        guild.loadMembers().onSuccess { members ->
            for (repo in repos) {
                val user = members.firstOrNull { it.user.id == repo.ownerId }
                if (user == null) {
                    println("${repo.ownerId} not found in user list")
                    continue
                }

                jda.getTextChannelById(discordProperties.pluginHubChannelId)
                    ?.editMessageEmbedsById(repo.messageId!!, createEmbed(user, repo))
                    ?.submit(false)
                    ?.whenComplete { _, err ->
                        if (err != null) {
                            restService.put("/repos/${repo.repoId}?messageId=&ownerId=${repo.ownerId}", null)
                        }
                    }
            }
        }
    }

    private fun sendMessage(user: Member, repo: PluginRepoDto): Message? {
        return jda.getTextChannelById(discordProperties.pluginHubChannelId)
            ?.sendMessageEmbeds(createEmbed(user, repo))
            ?.complete(false)
    }

    private fun createEmbed(user: Member, repo: PluginRepoDto) = embedHelper.builder("Unethicalite Hub")
        .setThumbnail(user.avatarUrl)
        .addField("Owner", user.user.asMention, false)
        .addField("URL", repo.repoUrl, false)
        .addField("Repo owner", repo.repoOwnerName, false)
        .addField("Repo name", repo.repoName, false)
        .addField("Plugins", repo.plugins.size.toString(), false)
        .build()
}