/*
 * Copyright 2023 Innové Gen AI
 *
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
