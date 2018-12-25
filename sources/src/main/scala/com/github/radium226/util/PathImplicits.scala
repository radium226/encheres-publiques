package com.github.radium226.util

import java.nio.file.{Path, Paths}

trait PathImplicits {

  implicit def stringToPath(pathAsString: String): Path = {
    Paths.get(pathAsString)
  }

}

object PathImplicits extends PathImplicits
