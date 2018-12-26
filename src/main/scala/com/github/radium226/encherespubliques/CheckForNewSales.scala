package com.github.radium226.encherespubliques

import cats.effect.IO
import com.github.radium226.browsing.Browser

import scala.concurrent._
import com.github.radium226.Implicits._
import com.typesafe.config.ConfigFactory
import io.tmos.arm.ArmMethods._

object CheckForNewSales extends App {

  implicit val timer = IO.timer(ExecutionContext.global)

  implicit val config = ConfigFactory.load("/etc/encheres-publiques.conf")

  import Interactions._

  val token = config.getString("token")

  for (
    browser   <- manage(Browser.open());
    notifyBot <- manage(NotifyBot.start(token))
  ) yield {
    for {
      scrapedLinks <- browser.browse(for {
      _            <- go()
      _            <- enterCriteria()
      links        <- scrapeLinks()
      } yield links)
      visitedLinks <- Link.load("./links.txt")
      linksToVisit  = scrapedLinks diff visitedLinks
      _            <- Link.dump("./DEBUG-linksToVisit.txt", linksToVisit)
      _            <- Link.dump("./DEBUG-visitedLinks.txt", visitedLinks)
      _            <- Link.dump("./links.txt", scrapedLinks)
      _            <- notifyBot.notifyLinks(linksToVisit)
    } yield linksToVisit
  }.failed.foreach(throw _)

}
