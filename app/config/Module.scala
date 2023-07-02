/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package config

import com.google.inject.AbstractModule
import utils.StartUpAction

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[StartUpAction]).asEagerSingleton()
  }
}