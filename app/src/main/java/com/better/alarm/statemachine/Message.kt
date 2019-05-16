package com.better.alarm.statemachine

import com.better.alarm.util.Optional

/**
 * Created by Yuriy on 07.03.2017.
 */
data class Message
(
        val what: Int,
        /** aux  */
        val handler: IHandler,
        val arg1: Int? = null,
        val arg2: Int? = null,
        val obj: Any? = null
) {
    fun what(): Int = what

    /** aux  */
    private fun handler(): IHandler = handler

    fun arg1(): Optional<Int> = Optional.fromNullable(arg1)

    fun arg2(): Optional<Int> = Optional.fromNullable(arg2)

    fun obj(): Optional<Any> = Optional.fromNullable(obj)

    fun send() {
        handler().sendMessage(this)
    }

    fun sendAtFront() {
        handler().sendMessageAtFrontOfQueue(this)
    }

    override fun toString(): String {
        return "Message[$what]{${listOfNotNull(arg1, arg2, obj).joinToString(separator = ", ")}}]"
    }

    fun formatToString(converter: (Int) -> String): String {
        return "Message[${converter.invoke(what)}]{${listOfNotNull(arg1, arg2, obj).joinToString(separator = ", ")}}"
    }

    fun withObj(obj: Any) = copy(obj = obj)
    fun withArg1(arg1: Int) = copy(arg1 = arg1)
    fun withArg2(arg2: Int) = copy(arg2 = arg2)

    companion object {
        @JvmStatic
        fun create(what: Int, handler: IHandler) = Message(what, handler)
    }
}
