package com.github.radium226.encherespubliques

import java.io.PrintStream
import java.nio.file.{Files, Paths}

import com.github.radium226.browsing._
import org.openqa.selenium.firefox.FirefoxDriver
import cats._
import cats.implicits._

import scala.util.{Failure, Success}
import scala.concurrent.duration._

object ExportOldSales extends App {

  import Browsing._

  case class Sale(date: String, propertyType: String, startPrice: String, endPrice: String, url: String)

  def scrapeSale(tr: Element): Browsing[Sale] = for {
    tds <- tr.findElements("td")
    date = tds(0).text
    propertyType = tds(2).text
    startPrice = tds(4).text
    endPrice = tds(5).text
    a <- tds(2).findElement("a[itemprop='url']")
    url = a.href
  } yield Sale(date, propertyType, startPrice, endPrice, url)

  def scrapeSales(currentPageNumber: Int = 0, numberOfPagesToScrape: Int = Int.MaxValue): Browsing[List[Sale]] = for {
    _         <- sleep(10 seconds)
    table     <- findElement("html body table tbody tr td div table tbody tr td table.ariallight11")
    oddTrs    <- table.findElements("tr.tr_normal")
    evenTrs   <- table.findElements("tr.tr_normal2")
    trs        = oddTrs ++ evenTrs
    //_          = println(trs)
    sales     <- trs.traverse(scrapeSale)
    nextSales <- if (currentPageNumber < numberOfPagesToScrape - 1) {
      for {
        exists    <- elementExists("img[alt='Page suivante']")
        nextSales <- if (exists) {
          for {
            element   <- findElement("img[alt='Page suivante']")
            _         <- element.click()
            nextSales <- scrapeSales(currentPageNumber + 1, numberOfPagesToScrape)
          } yield nextSales
        } else {
          Browsing { _ => List.empty[Sale] }
        }
      } yield nextSales
    } else {
        Browsing { _ => List.empty[Sale] }
    }
  } yield sales ++ nextSales

  val browsing = for {
    _      <- goTo("http://www.encheres-publiques.com/")
    img    <- findElement("#img_menu_3")
    _      <- img.click()
    select <- findElement("#select_region select[name='id_region']")
    _      <- select.select("Ile-de-France")
    form   <- findElement("form[name='frmCarnet']")
    _      <- form.submit()
    sales  <- scrapeSales()
  } yield sales

  for (browser <- Browser.managed()) yield {
    browser.browse(browsing) match {
      case Success(sales) =>
        var printer: PrintStream = null
        try {
          printer = new PrintStream(Files.newOutputStream(Paths.get("./oldSales.txt")))
          sales
            .map(_.productIterator.toList)
            .map(_.mkString(";"))
            .foreach(printer.println)
        } finally {
          if (printer != null) printer.close()
        }

      case Failure(throwable) =>
        throw throwable
    }
  }

}
