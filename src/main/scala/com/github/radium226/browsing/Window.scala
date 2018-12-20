package com.github.radium226.browsing

import scala.io.Source

case class Window(id: String)

object Window {

  def active(implicit display: Display): Window = {
    val activeWindowID = xdotool("getactivewindow")
    println(s"activeWindowID=${activeWindowID}")
    Window(activeWindowID.head)
  }

  def maximize(window: Window)(implicit display: Display): Unit = {
    xdotool("windowsize", window.id, "100%", "100%")
  }

}
