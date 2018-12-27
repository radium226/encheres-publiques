package com.github.radium226.experimental

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import com.github.radium226.browsing.{Browser, Browsing}
import com.typesafe.config.{Config, ConfigFactory, ConfigValue, ConfigValueFactory}
import io.tmos.arm.ArmMethods.manage
import scopt.Read

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

// radium.Main --module-name=*
object Main extends App {

  def dateValueReader = new ValueReader[LocalDate] {

    override def read(config: Config, path: String): LocalDate = {
      val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
      LocalDate.parse(config.getString(path), dateTimeFormatter)
    }
  }

  implicit class PimpedOption[T](option: Option[T]) {

    def toTry: Try[T] = toTry(new NoSuchElementException())

    def toTry(throwable: Throwable) = option.map(Success(_)).getOrElse(Failure(throwable))

  }

  implicit class PimpedListOfString(list: List[String]) {

    def glob(regex: String): List[String] = {
      val pattern = Pattern.compile(regex)
      list.filter(pattern.matcher(_).find())
    }

  }

  case class Environment(config: Config) {

    def lastDate: LocalDate = config.as[LocalDate]("last-date")(dateValueReader)

  }

  object Environment {

    trait Parser {

      def parse(arguments: List[String]): Try[Environment]

    }

    def default: Environment = {
      Environment(ConfigFactory.empty().withValue("last-date", ConfigValueFactory.fromAnyRef("2018/12/01")))
    }

    def parse(arguments: List[String]): IO[Environment] = IO {
      val parser = new scopt.OptionParser[Environment]("experiment") {

      }

      parser.parse(arguments, Environment.default).getOrElse(throw new IllegalArgumentException())
    }

  }

  type Name = String

  case class Module(name: Name, browsing: Browsing[Unit])

  trait ModuleProvider {

    def provideModule(environment: Environment): IO[Module]

  }

  def moduleProviders: List[ModuleProvider] = List.empty

  def browse(module: Module): IO[Try[Unit]] = IO {
    for (browser <- Browser.managed(headless = false)) yield {
      browser.browse(module.browsing)
    }
  }

  override def main(arguments: Array[String]): Unit = {

    val io = for {
      environment <- Environment.parse(arguments.toList)
      modules     <- moduleProviders.traverse(_.provideModule(environment))
      tries       <- modules.traverse(browse(_))
    } yield tries

    io.attempt.unsafeRunSync().toTry.flatMap(_.sequence).failed.foreach(throw _)
  }


}
