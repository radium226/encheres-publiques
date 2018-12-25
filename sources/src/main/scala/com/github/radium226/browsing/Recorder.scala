package com.github.radium226.browsing

import java.nio.file.Path

import com.github.radium226.util.ProcessImplicits._
import com.github.radium226.browsing.BrowsingImplicits._
import io.tmos.arm.ArmMethods._
import io.tmos.arm.CanManage

case class Recorder(videoFilePath: Path, ffmpegProcess: Process)

object Recorder {

  def start(videoFilePath: Path)(implicit display: Display) = {
    val ffmpegCommand = Seq(
      "ffmpeg",
      "-y",
      "-framerate", "25",
      "-f", "x11grab",
      "-s", s"${display.size.width}x${display.size.height}",
      "-i", s":${display.number}.0",
      videoFilePath.toString
    )

    val ffmpegProcess = new ProcessBuilder()
        .command(ffmpegCommand: _*)
        .inheritIO()
        .start()

    Recorder(videoFilePath, ffmpegProcess)
  }

  def stop(recorder: Recorder): Unit = {
    val ffmpegProcess = recorder.ffmpegProcess
    ffmpegProcess.kill("INT")
    ffmpegProcess.waitFor()
  }

  def managed(videoFilePath: Path)(implicit display: Display) = {
    manage(Recorder.start(videoFilePath))(new CanManage[Recorder] {

      override def onFinally(recorder: Recorder): Unit = Recorder.stop(recorder)

    })
  }

}
