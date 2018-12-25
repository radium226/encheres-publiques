package com.github.radium226.experimental

import java.util.regex.Pattern

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import scopt.Read

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

trait ScoptImplicits {

  implicit def readRegex = new Read[Regex] {

    def arity = 1

    def reads = { regex =>
      new Regex(regex)
    }

  }

}

object ScoptImplicits extends ScoptImplicits

import ScoptImplicits._

// radium.Main --module-name=*
object Main extends App {

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

  case class Environment(action: String, moduleNameRegex: Regex, arguments: List[String] = List.empty[String]) {

    def matchesModule(module: Module): Boolean = {
      moduleNameRegex.findFirstIn(module.name).isDefined
    }

  }

  object Environment {

    trait Parser {

      def parse(arguments: List[String]): Try[Environment]

    }

    def default: Environment = {
      Environment(
        action = "list-modules",
        moduleNameRegex = "^.*$".r
      )
    }

    def parser(modules: List[Module]): Parser = new Parser {

      override def parse(arguments: List[String]): Try[Environment] = {
        val parser = new scopt.OptionParser[Environment]("update-checker") {
          opt[Regex]('n', "module-name").action { (moduleNameRegex, environment) =>
            environment.copy(moduleNameRegex = moduleNameRegex)
          }

          cmd("list-modules").action { (_, environment) =>
            environment.copy(action = "list-modules")
          }

        }



        parser.parse(arguments, Environment.default).toTry(new IllegalArgumentException())
      }

    }

  }

  type Name = String

  type Program = ReaderT[IO, Environment, Try[Unit]]

  case class Module(name: Name, program: Program) {

    def runProgram(environment: Environment): Try[Unit] = {
      program.run(environment).attempt.unsafeRunSync().toTry.flatten
    }

  }

  trait ModuleProvider {

    def provideModule(): Try[Module]

  }

  def moduleProviders: List[ModuleProvider] = List.empty[ModuleProvider]

  trait Command {

    def apply(module: Module): Environment => Try[Unit] = ???

  }

  case object ListModules extends Command {

    def apply(module: Module) = { environment =>

    }

  }

  case object RunModules {

  }

  case class Parser(modules: List[Module]) {

    def parse(arguments: Seq[String]): Try[(Environment, Command)] = ???

  }

  override def main(arguments: Array[String]): Unit = {

    (for {
      modules                  <- moduleProviders.traverse(_.provideModule())
      parser                    = Parser(modules)
      (environment, command)   <- parser.parse(arguments.toList)
      matchedModules            = modules.filter(environment.matchesModule)
      _                        <- matchedModules.traverse(command(_)(environment))
    } yield ()).failed.foreach(throw _)
  }


}