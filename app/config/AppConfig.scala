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
  val reviewsFilepath: String = config.get[String]("dataset.reviews.filepath")
  val imagesFilepath: String = config.get[String]("dataset.images.filepath")
}
