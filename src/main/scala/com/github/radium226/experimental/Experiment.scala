package com.github.radium226.experimental

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

object Experiment extends App {

  implicit class PimpedStream[T](stream: Stream[T]) {

    def peak(f: T => Unit): Stream[T] = {
      stream.map({ t => f(t) ; t })
    }

  }

  object Interactions {

    def scrapeArticles(): IO[Stream[Article]] = IO {
      println("Generating articles... ")
      (new Iterator[Article] {

        var number = 0

        override def hasNext: Boolean = true

        override def next(): Article = {
          val article = Article(number)

          println(s"GENERATING ARTICLE #${article}")

          number = number + 1
          article
        }

      })
        .toStream
    }

    def scrapeCategoryByArticle(article: Article): IO[Stream[Category]] = IO {
      println(s"Generating categories for article #${article.number}... ")
      Iterator.range(0, 3)
        .map(Category(article.number, _))
        .toStream

    }

    def scrapeVideoByCategory(category: Category): IO[Stream[Video]] = IO {
      println(s"Generating videos for category #${category.number} and article #${category.articleNumber}... ")
      Iterator.range(0, 3)
        .map(Video(category.articleNumber, category.number, _))
        .toStream
    }

  }

  case class Article(number: Int)

  case class Category(articleNumber: Int, number: Int)

  case class Video(articleNumber: Int, categoryNumber: Int, number: Int)

  import Interactions._

  scrapeArticles()
    .flatMap(_.takeWhile(_.number <= 1).flatTraverse(scrapeCategoryByArticle(_)
        .flatMap(_.flatTraverse(scrapeVideoByCategory(_)))))
          .unsafeRunSync()
            .foreach(println)

}
