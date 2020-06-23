package io.github.nekohasekai.user

import cn.hutool.log.level.Level
import io.github.nekohasekai.nekolib.cli.TdCli
import io.github.nekohasekai.nekolib.cli.TdLoader
import io.github.nekohasekai.nekolib.cli.bl.ExportBinlog
import io.github.nekohasekai.nekolib.core.client.TdClient
import io.github.nekohasekai.nekolib.core.raw.deleteChatMessagesFromUser
import io.github.nekohasekai.nekolib.core.raw.getChat
import io.github.nekohasekai.nekolib.core.raw.getUser
import io.github.nekohasekai.nekolib.core.utils.*
import io.github.nekohasekai.user.tools.CleanDA
import io.github.nekohasekai.user.tools.DelAll
import io.github.nekohasekai.user.tools.DelMe
import io.github.nekohasekai.user.tools.UpgradeToSupergroup
import td.TdApi

object Launcher : TdCli() {

    lateinit var parameters: Array<String>

    override val loginType = LoginType.USER

    init {

        options databaseDirectory "data/main"

    }

    @JvmStatic
    fun main(args: Array<String>) {

        parameters = args

        TdLoader.tryLoad()

        start()

    }

    override fun onLoad() {

        if (parameters.isNotEmpty()) {

            when (parameters.getOrNull(0)) {

                "export" -> {

                    addHandler(ExportBinlog())

                    return

                }

            }

        }

        addHandler(CleanDA())

        addHandler(DelMe())

        addHandler(DelAll())

        addHandler(UpgradeToSupergroup())

    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        super.onNewMessage(userId, chatId, message)

        if (userId == 0) return

        if (chatId == -1001432997913L) {

            if (message.content is TdApi.MessagePinMessage) {

                val user = getUser(userId)
                
                if (user.isBot) {

                    deleteChatMessagesFromUser(chatId, userId)

                    return

                }
                
                sudo delete message

                return

            }

        }

        message.text?.also {

            defaultLog.debug("[${getChat(chatId).title}] ${getUser(userId).displayName}: ${message.text}")

        }

        message.content.takeIf { it is TdApi.MessageSticker }?.also {

            defaultLog.debug("[${getChat(chatId).title}] ${getUser(userId).displayName}: [Sticker ${(it as TdApi.MessageSticker).sticker.emoji}]")

        }

    }

}