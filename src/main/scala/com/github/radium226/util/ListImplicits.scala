package com.github.radium226.util

import scala.util.{Success, Try}

trait ListImplicits {

  implicit class PimpedList[T](list: List[Try[T]]) {

    def collectSuccesses(): List[T] = {
      list.collect({
        case Success(t) =>
          t
      })
    }

  }

}

object ListImplicits extends ListImplicits
