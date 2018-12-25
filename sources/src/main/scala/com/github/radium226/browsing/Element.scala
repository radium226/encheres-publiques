package com.github.radium226.browsing

import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.support.ui.Select

import scala.collection.JavaConverters._

case class Element(webElement: WebElement) {

  def href: String = {
    webElement.getAttribute("href")
  }

  def text: String = {
    webElement.getText()
  }

  def findElement(cssSelector: CSSSelector): Browsing[Element] = Browsing { browser =>
    Element(webElement.findElement(By.cssSelector(cssSelector)))
  }

  def findElements(cssSelector: CSSSelector): Browsing[List[Element]] = Browsing { browser =>
    webElement.findElements(By.cssSelector(cssSelector)).asScala.toList.map(Element.apply)
  }

  def enter(text: String): Browsing[Unit] = Browsing { browser =>
    webElement.sendKeys(text)
  }

  def click(): Browsing[Unit] = Browsing { browser =>
    webElement.click()
  }

  def select(text: String): Browsing[Unit] = Browsing { browser =>
    new Select(webElement).selectByVisibleText(text)
  }

  def submit(): Browsing[Unit] = Browsing { browser =>
    webElement.submit()
  }

}
