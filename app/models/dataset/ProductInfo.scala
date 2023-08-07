/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models.dataset

import play.api.libs.json.{Json, OFormat}

case class ProductInfo(product_id: String,
                       product_name: String,
                       brand_id: String,
                       brand_name: String,
                       loves_count: Option[Int],
                       rating: Option[String],
                       reviews: Option[Int],
                       size: Option[String],
                       variation_type: Option[String],
                       variation_value: Option[String],
                       ingredients: String,
                       price_usd: String,
                       value_price_usd: Option[String],
                       sale_price_usd: Option[String],
                       `new`: Boolean,
                       online_only: Boolean,
                       highlights: String,
                       primary_category: String,
                       secondary_category: Option[String],
                       tertiary_category: Option[String],
                       image: Option[String])

object ProductInfo {
  implicit val formats: OFormat[ProductInfo] = Json.format[ProductInfo]
}
