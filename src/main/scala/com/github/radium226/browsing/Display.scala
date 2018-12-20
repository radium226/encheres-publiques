package com.github.radium226.browsing

import com.github.radium226.util.ProcessImplicits._

class Display(val size: Size, val number: Int, val xvfbProcess: Process) {

  def close(): Unit = {
    xvfbProcess.kill("INT")
    xvfbProcess.waitFor()
  }

}

object Display {

  val defaultNumber = 42

  val defaultSize = Size(1024, 768)

  def open(size: Size = defaultSize, number: Int = defaultNumber): Display = {
    val xvfbProcess = new ProcessBuilder()
        .command("Xvfb", s":${number}", "-screen", "0", s"${size.width}x${size.height}x24")
      .start()

    new Display(size, number, xvfbProcess)
  }

}

