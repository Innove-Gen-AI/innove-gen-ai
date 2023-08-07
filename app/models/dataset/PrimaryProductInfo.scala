/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models.dataset

import play.api.libs.json.{Json, OFormat}

case class PrimaryProductInfo(product_id: String,
                              product_name: String,
                              brand_name: String,
                              image: Option[String])

object PrimaryProductInfo {
  implicit val formats: OFormat[PrimaryProductInfo] = Json.format[PrimaryProductInfo]
}
