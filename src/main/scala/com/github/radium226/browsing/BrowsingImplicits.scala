package com.github.radium226.browsing

import com.github.radium226.encherespubliques.NotifyBot
import io.tmos.arm.CanManage

trait BrowsingImplicits {

  implicit class PimpedBrowsing[O](browsing: Browsing[O]) {

    def waitFor()(implicit timeout: WaitForTimeout): Browsing[O] = {
      Browsing.waitFor(browsing)
    }

  }

  implicit def canManageBrowser = new CanManage[Browser] {

    override def onFinally(browser: Browser): Unit = {
      browser.close()
    }

  }

  implicit def canManageNotifyBot = new CanManage[NotifyBot] {

    override def onFinally(notifyBot: NotifyBot): Unit = {
      notifyBot.shutdown()
    }

  }

}

object BrowsingImplicits extends BrowsingImplicits
