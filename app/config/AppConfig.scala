/*
 * Copyright 2023 Innové Gen AI
 *
 */

package config

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(config: Configuration) {

  val productsFilepath: String = config.get[String]("dataset.products.filepath")

  val reviewsFilepath: String = {
    if(config.get[Boolean]("dataset.limit")){
      config.get[String]("dataset.test.reviews.filepath")
    } else {
      config.get[String]("dataset.reviews.filepath")
    }
  }
}
