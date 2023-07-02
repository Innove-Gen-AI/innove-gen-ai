/*
 * Copyright 2020 HM Revenue & Customs
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
 */

package models.dataset

import play.api.libs.json.{Json, OFormat}

case class ProductReview(author_id: String,
                        rating: Int,
                        is_recommended: String,
                        helpfulness: String,
                        total_feedback_count: Int,
                        total_neg_feedback_count: Int,
                        total_pos_feedback_count: Int,
                        submission_time: String,
                        review_text: String,
                        review_title: String,
                        skin_tone: String,
                        eye_color: String,
                        skin_type: String,
                        hair_color: String,
                        product_id: String,
                        product_name: String,
                        brand_name: String,
                        price_usd: Double)

object ProductReview {
  implicit val formats: OFormat[ProductReview] = Json.format[ProductReview]
}
