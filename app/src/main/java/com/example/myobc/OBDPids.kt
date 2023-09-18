package com.example.myobc

import com.github.eltonvs.obd.command.ATCommand
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.bytesToInt

/**
 * Declaration of other OBD command.
 * In this class are specified commands that are not included in the library, specially init codes
 */

class SetBaudRateCommand(baud: Int) : ATCommand() {
    override val tag = "BAUD"
    override val name = "Set baud rate"
    override val mode = "AT"
    override val pid = "PB38400"
}

class HeadersCommand(header: Switcher) : ATCommand() {
    override val tag = "HEADER"
    override val name = "Set at header"
    override val mode = "AT"
    override val pid = "H0"
}

class SetMemoryCommand(value: Switcher) : ATCommand() {
    override val tag = "MEMORY"
    override val name = "Memory on/off"
    override val mode = "AT"
    override val pid = "M0"
}

class DeviceDescriptorCommand : ATCommand() {
    override val tag = "DESC"
    override val name = "Device descriptor"
    override val mode = "AT"
    override val pid = "@1"
}

class DisplayProtoNumberCommand : ATCommand() {
    override val tag = "DPROTO"
    override val name = "Dispaly protocol number"
    override val mode = "AT"
    override val pid = "DPN"
}

class DeviceInfoCommand : ATCommand() {
    override val tag = "INFO"
    override val name = "Device information"
    override val mode = "AT"
    override val pid = "I"
}



class RPMCommandFix : ObdCommand() {
    override val tag = "ENGINE_RPM"
    override val name = "Engine RPM"
    override val mode = "01"
    override val pid = "0C"

    override val defaultUnit = "RPM"
    override val handler = { it: ObdRawResponse -> bytesToInt(it.bufferedValue, bytesToProcess = 2 ).toString()}
}
