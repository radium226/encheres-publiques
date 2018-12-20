package com.github.radium226.browsing

import scala.io.Source

object xdotool {

  def apply(command: String*)(implicit display: Display): Seq[String] = {
    val xdotoolProcessBuilder = new ProcessBuilder()
    xdotoolProcessBuilder.environment().put("DISPLAY", s":${display.number}")
    xdotoolProcessBuilder.command(("xdotool" +: command): _*)

    val xdotoolProcess = xdotoolProcessBuilder.start()
    val stdout = Source.fromInputStream(xdotoolProcess.getInputStream).getLines().toList
    xdotoolProcess.waitFor()
    stdout
  }

}
