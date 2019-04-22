package com.better.alarm.model

import com.better.alarm.logger.Logger
import com.better.alarm.statemachine.HandlerFactory
import com.better.alarm.statemachine.IHandler
import com.better.alarm.statemachine.Message
import com.better.alarm.statemachine.MessageHandler

class ImmediateHandlerFactory : HandlerFactory {
    override fun create(messageHandler: MessageHandler): IHandler {
        return object : IHandler {
            val messages = mutableListOf<Message>()

            override fun sendMessageAtFrontOfQueue(message: Message) {
                sendMessage(message, true)
            }

            override fun sendMessage(message: Message) {
                sendMessage(message, false)
            }

            private fun sendMessage(message: Message, front: Boolean) {
                when {
                    messages.isEmpty() -> {
                        Logger.getDefaultLogger().d("Immediate $message")
                        messageHandler.handleMessage(message)
                    }
                    front -> {
                        Logger.getDefaultLogger().d("Queue at front $message")
                        messages.add(0, message)
                    }
                    else -> {
                        Logger.getDefaultLogger().d("Queue $message")
                        messages.add(message)
                    }
                }
                while (messages.isNotEmpty()) {
                    Logger.getDefaultLogger().d("Queued $messages")
                    messageHandler.handleMessage(messages.removeAt(0))
                }
            }

            override fun obtainMessage(what: Int, obj: Any): Message {
                return Message(what, this, null, null, obj)
            }

            override fun obtainMessage(what: Int): Message {
                return Message(what, this, null, null, null)
            }
        }
    }
}