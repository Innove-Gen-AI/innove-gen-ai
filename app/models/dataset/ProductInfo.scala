/*
 * Copyright 2023 Innové Gen AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
