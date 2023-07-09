package com.example.myobc

import com.github.eltonvs.obd.command.ATCommand
import com.github.eltonvs.obd.command.Switcher


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

