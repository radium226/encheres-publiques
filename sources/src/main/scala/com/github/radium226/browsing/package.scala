package com.github.radium226

import cats.data.ReaderT
import cats.effect.IO

package object browsing {

  type Browsing[O] = ReaderT[IO, Browser, O]

  type CSSSelector = String

}
