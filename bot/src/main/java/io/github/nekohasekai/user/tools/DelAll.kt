package io.github.nekohasekai.user.tools

import io.github.nekohasekai.nekolib.core.client.TdClient
import io.github.nekohasekai.nekolib.core.client.TdHandler
import io.github.nekohasekai.nekolib.core.raw.deleteMessages
import io.github.nekohasekai.nekolib.core.raw.getChat
import io.github.nekohasekai.nekolib.core.utils.*
import io.github.nekohasekai.nekolib.i18n.LocaleController
import io.github.nekohasekai.nekolib.i18n.UNKNOWN_PARAMETER
import kotlinx.coroutines.runBlocking
import td.TdApi

class DelAll : TdHandler() {

    override fun onLoad() {

        initFunction("del_all")

    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {

        if (!isMyMessage(message)) return

        doDelAll(sudo, chatId, message, params)

    }

}

suspend fun TdHandler.doDelAll(anchor: TdClient, chatId: Long, message: TdApi.Message, params: Array<String>) {

    var all = true
    var sticker = false
    var serviceMessage = false
    var forward = false
    var document = false

    var hide = false
    var keepChannel = false

    params.forEach {

        if (it == "-s" || it == "--sticker") {

            all = false
            sticker = true

        } else if (it == "-m" || it == "--service-message") {

            all = false
            serviceMessage = true

        } else if (it == "-d" || it == "--document") {

            all = false
            document = true

        } else if (it == "-f" || it == "--forward") {

            all = false
            forward = true

        } else if (it == "-k" || it == "--keep-channel") {

            keepChannel = true

        } else if (it == "-h" || it == "--hide") {

            hide = true

        } else {

            sudo make LocaleController.UNKNOWN_PARAMETER.input(it) onSuccess deleteDelay(message) replyTo message

            return

        }

    }

    val title = anchor.getChat(chatId).title

    val status = if (!hide && anchor == sudo) {
        if (message.canBeEdited) {
            sudo make "Deleting..." syncEditTo message
        } else {
            sudo make "Deleting..." syncReplyTo message
        }
    } else {
        sudo delete message
        sudo make "Deleting from $title..." syncTo me.id
    }

    var deleted = 0
    var offset = 0

    val deletePool = mkFastPool()

    anchor.fetchMessages(chatId, message.replyToMessageId) { msg ->

        offset += msg.size

        msg.filter {
            it.canBeDeletedForAllUsers && it.id != message.id && (
                    (!keepChannel || it.senderUserId == 0) && (all ||
                            (sticker && it.content is TdApi.MessageSticker) ||
                            (serviceMessage && it.isServiceMessage) ||
                            (document && it.content is TdApi.MessageDocument) ||
                            (forward && it.forwardInfo != null)
                            )
                    )
        }.map { it.id }
                .toLongArray()
                .takeIf { it.isNotEmpty() }
                ?.also { anchor.deleteMessages(chatId, it, true) }
                ?.also { deleted += it.size }

        deletePool.executeTimed {

            runBlocking {

                sudo make "${if (!hide) "Deleting" else "Deleting from $title"} ($deleted / $offset) ..." syncEditTo status

            }

        }

        true

    }

    deletePool.executeTimed {

        sudo make "Deleted $deleted messages${if (hide) " at $title" else ""}." onSuccess withDelay {

            if (anchor == sudo && !hide) delete(it)

            deletePool.shutdown()

        } editTo status

    }

}