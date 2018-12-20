package com.github.radium226.encherespubliques

import java.io.PrintStream
import java.nio.file.{Files, Path}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

import cats.data.IndexedStateT
import cats.effect._
import com.github.radium226.browsing.Browser

import scala.collection.JavaConverters._
import com.github.radium226.Implicits._
import io.tmos.arm.ArmMethods._
import io.tmos.arm.CanManage
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

import scala.concurrent.duration._

import scala.util._

object FixOldSales extends App {

  type PropertyType = String

  val PropertyTypes = List[PropertyType](
    "Appartement",
    "Maison",
    "Chambre",
    "Local commercial",
    "Local d'habitation",
    "Immeuble",
    "Garage",
    "Cave",
    "Studio",
    "Emplacement de stationnement",
    "Parking",
    "Divers",
    "Propriété",
    "Bâtiment",
    "Local industriel",
    "Local commercial et d'habitation",
    "Pavillon",
    "Box",
    "Logement",
    "Studette",
    "Pièce",
    "Local",
    "Parcelle de terre",
    "Ensemble immobilier",
    "Bien",
    "Terrain",
    "Boutique",
    "Boxes",
    "Loft",
    "Remise",
    "Emplacement clos",
    "Station-service"
  )

  type City = String

  case class Sale(date: LocalDate, propertyType: PropertyType, city: City, startPrice: Double, endPrice: Double, url: String, size: Option[Double])

  def readLines(filePath: Path): IO[Seq[String]] = IO {
    Files
      .lines(filePath)
      .iterator()
      .asScala
      .toSeq
  }

  def normalize(text: String): String = {
    val separators = "-' "
    var formattedText = text.toLowerCase
    separators.map(_.toString).foreach({ separator =>
      formattedText = formattedText.split(separator).map(_.capitalize).mkString(separator)
    })
    formattedText
  }

  def parseDate(cell: String): Try[LocalDate] = {
    "([0-9]{2})/([0-9]{2})/([0-9]{2})".r.findFirstIn(cell) match {
      case Some(dateAsString) =>
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
        Try(LocalDate.parse(dateAsString, dateTimeFormatter))

      case None =>
        Failure(new Exception(s"Unable to parse ${cell} as date"))
    }
  }

  def parsePropertyTypeAndCity(cell: String): Try[(PropertyType, City)] = {
    val propertyTypeGroup = PropertyTypes
      .map(Pattern.quote)
      .mkString("(", "|", ")")

    val cityGroup = "(.+)$"
    s"(?i)${propertyTypeGroup} ${cityGroup}".r("propertyType", "city").findFirstMatchIn(cell) match {
      case Some(firstMatch) =>
        val propertyType = firstMatch.group("propertyType")
        val city = firstMatch.group("city")
        Success((normalize(propertyType), normalize(city)))

      case None =>
        Failure(new Exception(s"Unable to parse ${cell} as property type and city"))
    }
  }

  def parsePrice(cell: String): Try[Double] = {
    "([0-9 .]+)€".r("price").findFirstMatchIn(cell) match {
      case Some(firstMatch) =>
        Try(firstMatch.group("price").replace(" ", "").toDouble)

      case None =>
        Failure(new Exception(s"Unable to parse ${cell} as price"))
    }
  }

  def parseSales(lines: Seq[String]): IO[Seq[Sale]] = IO {
    lines
      .toList
      .map(_.split(";").toList)
      .map({ cells =>
        for {
          date                 <- parseDate(cells(0))
          (propertyType, city) <- parsePropertyTypeAndCity(cells(1))
          startPrice           <- parsePrice(cells(2))
          endPrice             <- parsePrice(cells(3))
          url                  <- Try(cells(4))
        } yield Sale(date, propertyType, city, startPrice, endPrice, url, None)
      })
      .filter({
        case Success(_) =>
          true

        case Failure(throwable) =>
          println(throwable.getMessage)
          false
      })
      .collectSuccesses()
  }

  def formatDate(date: LocalDate): List[String] = {
    List(
      date.format(DateTimeFormatter.ofPattern("yyyy")),
      date.format(DateTimeFormatter.ofPattern("MM")),
      date.format(DateTimeFormatter.ofPattern("dd"))
    )
  }

  def formatPrice(price: Double): List[String] = {
    List(price.toString.replace(".", ","))
  }

  def formatSales(sales: Seq[Sale]): IO[Seq[String]] = IO {
    (List("Année", "Mois", "Jour", "Ville", "Type", "Surface", "Prix de départ", "Prix final", "URL") +: sales
      .map({ sale =>
        val date = formatDate(sale.date)
        val startPrice = formatPrice(sale.startPrice)
        val endPrice = formatPrice(sale.endPrice)
        val city = List(sale.city)
        val propertyType = List(sale.propertyType)
        val url = List(sale.url)

        val size = List(sale.size.map(_.toString.replace(".", ",")).getOrElse(""))

        date ++ city ++ propertyType ++ size ++ startPrice ++ endPrice ++ url
      }))
      .map(_.mkString(";"))
  }

  def writeLines(lines: Seq[String], filePath: Path) = IO {
    for (printer <- manage(new PrintStream(Files.newOutputStream(filePath)))) {
      lines.foreach({ line =>
        println(line)
        printer.println(line)
      })
    }
  }

  implicit def canManageWebDriver = new CanManage[WebDriver] {

    override def onFinally(webDriver: WebDriver): Unit = {
      webDriver.close()
    }

  }

  def scrapeSizes(sales: Seq[Sale]): IO[Seq[Sale]] = IO {
    import com.github.radium226.browsing._
    import com.github.radium226.browsing.Browsing._

    implicit val waitForTimeout = WaitForTimeout(10 seconds)

    def scrape(): Browsing[Option[Double]] = {
      waitFor(findElement("#div_desc"))
        .map({ div =>
          val text = div.text
          //println(text)
          "(?is)([0-9,]+) ?m[2²]".r("size").findFirstMatchIn(text)
            .map({ firstMatch =>
              //println(firstMatch)
              firstMatch.group("size").replace(",", ".").toDouble
            })
        })
    }

    for (browser <- manage(Browser.open())) yield {
      sales
        .map({ sale =>
          val browsing = for {
            _    <- goTo(sale.url)
            _    <- sleep(3 seconds)
            size <- scrape()
          } yield size

          val size = browser.browse(browsing) match {
            case Success(Some(size)) =>
              Some(size)

            case Success(None) =>
              None

            case Failure(exception) =>
              exception.printStackTrace()
              None
          }

          (sale, size)
        })
        .map({ case (sale, size) =>
          sale.copy(size = size)
        })
        .toList
    }
  }

  val filePath = "./oldSales.txt"
  val fixedFilePath = "./oldSales-FIXED.txt"
  (for {
    lines          <- readLines(filePath)
    sales          <- parseSales(lines)
    salesWithSize  <- scrapeSizes(sales)
    formattedLines <- formatSales(salesWithSize)
    _              <- writeLines(formattedLines, fixedFilePath)
  } yield ()).unsafeRunSync()

}
