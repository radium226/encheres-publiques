package com.github.radium226.encherespubliques

import com.github.radium226.Implicits._
import com.github.radium226.browsing._

import scala.concurrent.duration._
import scala.util._

import cats._
import cats.implicits._


object Interactions {

  implicit val waitForTimeout = WaitForTimeout(10 seconds)

  import Browsing._

  def go(): Browsing[Unit] = {
    goTo("http://www.encheres-publiques.com/")
  }

  def enterCriteria(): Browsing[Unit] = for {
    input  <- findElement("#select_region select[name='id_region']").waitFor
    _      <- input.click()
    _      <- input.select("Ile-de-France")
    button <- findElement("#fond_index_recherche form table tbody tr td a")
    _      <- button.click()
  } yield ()

  private def goToNextPage(): Browsing[Boolean] = {
    val cssSelector = "a[title='Page suivante']"
    elementExists(cssSelector)
      .flatMap({ exists =>
        if (exists) {
          //println("Ça existe")
          for {
            a <- findElement(cssSelector)
            _ <- a.click()
          } yield true
        } else {
          //println("Ça n'existe pas")
          Browsing { _ => false }
        }
      })
  }

  def scrapeLink(table: Element): Browsing[Try[Link]] = for {
    a    <- table.findElement("a[itemprop='url']")
    span <- table.findElement("span[itemprop='lowPrice']")
    link = Link.parse(a, span)
  } yield link

  def scrapeLinksOnCurrentPage(): Browsing[List[Link]] = for {
    tables <- findElements("form[name='frmVentes'] table")
    links  <- tables.traverse[Browsing, Try[Link]]({ table => scrapeLink(table) })
  } yield links.collectSuccesses()

  def scrapeLinks(numberOfPagesToScrape: Int = Int.MaxValue, currentPageNumber: Int = 0): Browsing[List[Link]] = {
    for {
      links       <- scrapeLinksOnCurrentPage()
      hasNextPage <- if (currentPageNumber < numberOfPagesToScrape - 1) goToNextPage() else Browsing { _ => false }
      nextLinks   <- if (hasNextPage) scrapeLinks(numberOfPagesToScrape, currentPageNumber + 1) else Browsing { _ => List.empty[Link] }
    } yield (links ++ nextLinks).distinct
  }

  def scrapeDescription(link: Link): Browsing[Description] = for {
    _           <- goTo(link.url)
    div         <- findElement("#div_desc")
    description  = Description(div.text)
  } yield description

}
