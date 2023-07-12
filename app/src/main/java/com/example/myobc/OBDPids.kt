package com.example.myobc

import com.github.eltonvs.obd.command.ATCommand
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.bytesToInt

/**
 * Declaration of other OBD command
 */

class SetBaudRateCommand(baud: Int) : ATCommand() {
    override val tag = "BAUD"
    override val name = "Set baud rate"
    override val mode = "AT"
    override val pid = "PB38400"
}
//TODO: use switcher
class HeadersCommand(header: Switcher) : ATCommand() {
    override val tag = "HEADER"
    override val name = "Set at header"
    override val mode = "AT"
    override val pid = "H0"
}

class RPMCommandFix : ObdCommand() {
    override val tag = "ENGINE_RPM"
    override val name = "Engine RPM"
    override val mode = "01"
    override val pid = "0C"

    override val defaultUnit = "RPM"
    override val handler = { it: ObdRawResponse -> bytesToInt(it.bufferedValue, bytesToProcess = 2 ).toString()}
}
