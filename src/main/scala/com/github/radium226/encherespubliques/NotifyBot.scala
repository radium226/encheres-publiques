package com.github.radium226.encherespubliques

import java.io.PrintStream
import java.nio.file.{Files, Paths}

import com.bot4s.telegram.api._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.methods._

import scala.collection.JavaConverters._
import scala.util.Try

object NotifyBot {

  def start(token: String): NotifyBot = {
    val notifyBot = new NotifyBot(token)
    notifyBot.run()
    notifyBot
  }

}

class NotifyBot(val token: String) extends TelegramBot with Polling with Commands with ChatActions {

  override val client = new ScalajHttpClient(token)

  def loadChatIds(): List[String] = {
    if (Files.exists(Paths.get("./chatIds.txt"))) {
      Files.readAllLines(Paths.get("./chatIds.txt")).asScala.toList.distinct
    } else {
      List.empty[String]
    }
  }

  def dumpChatIds(chatIds: List[String]) = {
    var printer: PrintStream = null
    try {
      printer = new PrintStream(Files.newOutputStream(Paths.get("./chatIds.txt")))
      chatIds.foreach({ chatId =>
        printer.println(chatId)
      })
    } finally {
      if (printer != null) printer.close()
    }
  }

  onCommand("/start") { implicit message =>
    dumpChatIds(loadChatIds() :+ message.source.toString)
    reply("Hello !")
  }

  def notifyLinks(linksToVisit: List[Link]): Try[Unit] = Try {
    loadChatIds().foreach({ chatId =>
      if (linksToVisit.size == 0) {
        request(SendMessage(chatId, s"Il n'y a pas de nouveaux biens !", disableWebPagePreview = Some(false)))
      } else {
        request(SendMessage(chatId, s"Il n'y a ${linksToVisit.size} nouveaux biens !"))
        linksToVisit.foreach({ linkToVisit =>
          request(SendMessage(chatId, linkToVisit.url, disableWebPagePreview = Some(false)))
        })
      }
    })
  }

}
