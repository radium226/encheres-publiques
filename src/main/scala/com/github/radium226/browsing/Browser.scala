package com.github.radium226.browsing

import java.io.PrintStream
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, FirefoxProfile}
import io.tmos.arm.ArmMethods._
import io.tmos.arm.ManagedResource
import BrowsingImplicits._

import scala.util.Try

object Browser {

  def managed(): ManagedResource[Browser] = {
    manage(Browser.open())
  }

  def open(): Browser = {
    implicit val display = Display.open()

    val firefoxProfile = new FirefoxProfile()
    firefoxProfile.setPreference("permissions.default.image", 2)

    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null")

    // Generate script with DISPLAY environment variable
    //FIXME
    val scriptContent =
      s"""#!/bin/sh
         |DISPLAY=":${display.number}" exec firefox "$${@}"
        """.stripMargin
    /*val scriptContent =
    s"""#!/bin/sh
       |exec firefox "$${@}"
        """.stripMargin*/
    val scriptFilePath = Files.createTempFile("firefox", ".sh")
    val printer = new PrintStream(Files.newOutputStream(scriptFilePath, StandardOpenOption.TRUNCATE_EXISTING))
    printer.println(scriptContent)
    printer.close()
    Files.setPosixFilePermissions(scriptFilePath, PosixFilePermissions.fromString("rwxr-xr-x"))

    // We set up the Firefox options
    val firefoxOptions = new FirefoxOptions()
    firefoxOptions.setBinary(scriptFilePath)
    firefoxOptions.setProfile(firefoxProfile)

    // We instantiate WebDriver
    val webDriver = new FirefoxDriver(firefoxOptions)
    val browser = new Browser(webDriver)

    val firefoxWindow = Window(xdotool("search", "--onlyvisible", "--sync", "--name", "Firefox").head)
    Window.maximize(firefoxWindow)

    browser
  }
}

class Browser(val webDriver: WebDriver)(implicit val display: Display) {

  def browse[O](browsing: Browsing[O]): Try[O] = {
    for (recorder <- Recorder.managed(Paths.get("./browsing.mp4"))) yield {
      browsing.run(this).attempt.unsafeRunSync().toTry
    }
  }

  def close(): Unit = {
    webDriver.close()
    display.close()
  }

  def withWebDriver(newWebDriver: WebDriver): Browser = {
    new Browser(newWebDriver)
  }

}
