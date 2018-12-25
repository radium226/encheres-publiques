package com.github.radium226.util

trait ProcessImplicits {

  implicit class PimpedProcess(process: Process) {

    def pid: Int = {
      process match {
        case unixProcess if unixProcess.getClass.getName.equals("java.lang.UNIXProcess") =>
          val field = unixProcess.getClass.getDeclaredField("pid")
          field.setAccessible(true)
          field.getInt(unixProcess)
        case _ =>
          -1
      }
    }

    def kill(signal: String): Unit = {
      val killCommand = Seq("kill", s"-SIG${signal}", pid.toString)
      val killProcess = new ProcessBuilder()
          .command(killCommand: _*)
        .start()

      killProcess.waitFor()
    }

  }

}

object ProcessImplicits extends ProcessImplicits
