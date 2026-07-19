package com.github.pires.obd.reader

import com.github.pires.obd.commands.ObdCommand

class ObdCommandJob(command: ObdCommand) {

    enum class ObdCommandJobState {
        NEW,
        RUNNING,
        FINISHED,
        EXECUTION_ERROR,
        BROKEN_PIPE,
        QUEUE_ERROR,
        NOT_SUPPORTED
    }

    var id: Long? = null
    val command: ObdCommand = command
    var state: ObdCommandJobState = ObdCommandJobState.NEW

    @JvmName("assignState")
    fun setState(newState: ObdCommandJobState) {
        this.state = newState
    }
}
