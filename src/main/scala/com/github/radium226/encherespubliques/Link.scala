package com.github.radium226.encherespubliques

import java.io.{BufferedWriter, OutputStreamWriter, PrintStream, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

import com.github.radium226.browsing.Element

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

object Link {

  def load(filePath: Path): Try[List[Link]] = Try {
    if (Files.exists(filePath)) {
      Files.readAllLines(filePath, StandardCharsets.UTF_8).asScala.toList
        .map(_.split(";"))
        .map({ segments =>
          (segments(0), segments(1), segments(2), segments(3).toFloat)
        })
        .map({ case(url, propertyType, city, lowPrice) =>
          Link(url, propertyType, city, lowPrice)
        })
    } else {
      List.empty[Link]
    }
  }

  def dump(filePath: Path, links: List[Link]): Try[Unit] = Try {
    var printer: PrintWriter = null
    try {
      printer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(filePath), StandardCharsets.UTF_8))
      links
          .map({ link =>
            Seq(link.url, link.propertyType, link.city, link.lowPrice.toString)
          })
          .map(_.mkString(";"))
          .foreach(printer.println)
    } finally {
      if (printer != null) printer.close()
    }
  }

  def parse(a: Element, span: Element): Try[Link] = {
    val url = a.href
    val text = a.text
    for {
      lowPrice <- Try { span.text.replace(" ", "").replace("€", "").toFloat }
      (propertyType, city) <- ("(.+) / (.+) - (.+)".r("propertyType", "zipCode", "city").findFirstMatchIn(text) match {
        case Some(firstMatch) =>
          Success((firstMatch.group("propertyType"), firstMatch.group("city")))
        case None =>
          println(s"text=${text}")
          Failure(new Exception(s"Unable to parse link from ${text}"))
      })
    } yield new Link(url, propertyType, city, lowPrice)
  }

}

case class Link(url: String, propertyType: String, city: String, lowPrice: Float)
