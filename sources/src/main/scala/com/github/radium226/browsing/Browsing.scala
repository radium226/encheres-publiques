package com.github.radium226.browsing

import java.util.concurrent.atomic.AtomicReference

import cats.data.ReaderT
import cats.effect.IO
import org.openqa.selenium.{By, WebDriver, WebElement}
import org.openqa.selenium.support.ui.{ExpectedCondition, Select, WebDriverWait}

import scala.concurrent.duration._
import scala.util.{Random, Success, Try}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Browsing {

  implicit val timer = IO.timer(ExecutionContext.global) // FIXME

  def goTo(url: String): Browsing[Unit] = Browsing { browser =>
    browser.webDriver.get(url)
  }

  def apply[O](block: Browser => O): Browsing[O] = ReaderT { browser =>
    for {
      _       <- IO.sleep(Random.nextInt((1000 - 500) + 1) milliseconds)
      outcome <- IO { block(browser) }
    } yield outcome
  }

  def findElement(cssSelector: CSSSelector): Browsing[Element] = Browsing { browser =>
    Element(browser.webDriver.findElement(By.cssSelector(cssSelector)))
  }

  def idle(): Browsing[Unit] = Browsing { browser =>

  }

  def elementExists(cssSelector: CSSSelector): Browsing[Boolean] = Browsing { browser =>
    Try(browser.webDriver.findElement(By.cssSelector(cssSelector))).isSuccess
  }

  def findElements(cssSelector: CSSSelector): Browsing[List[Element]] = Browsing { browser =>
    browser.webDriver
      .findElements(By.cssSelector(cssSelector)).asScala
      .toList
      .map(Element.apply)
  }

  def sleep(duration: FiniteDuration): Browsing[Unit] = ReaderT { _ =>
    IO.sleep(duration)
  }

  def waitFor[O](browsing: Browsing[O])(implicit timeout: WaitForTimeout): Browsing[O] = Browsing { browser =>
    val webDriver: WebDriver = browser.webDriver
    val outcomeRef = new AtomicReference[O]()
    new WebDriverWait(webDriver, timeout.duration.toSeconds)
      .until(new ExpectedCondition[Boolean] {

        override def apply(webDriver: WebDriver): Boolean = {
          browsing.run(browser.withWebDriver(webDriver)).attempt.unsafeRunSync().toTry match {
            case Success(outcome) =>
              outcomeRef.set(outcome)
              true

            case _ =>
              false
          }
        }

      })

    outcomeRef.get()
  }

}
