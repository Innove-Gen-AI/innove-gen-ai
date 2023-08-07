/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models.dataset

import play.api.libs.json.{Json, OFormat}

case class ProductImage(product_id: String,
                        image: String)

object ProductImage {
  implicit val formats: OFormat[ProductImage] = Json.format[ProductImage]
}
