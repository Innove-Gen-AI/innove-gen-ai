/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models

import play.api.libs.json.{Json, OFormat}

case class SentimentAnalysisResponse(predictions: Seq[Prediction]) extends GCPResponse

object SentimentAnalysisResponse {
  implicit val formats: OFormat[SentimentAnalysisResponse] = Json.format[SentimentAnalysisResponse]
}

case class SafetyAttributes(scores: Option[Seq[BigDecimal]],
                            blocked: Option[Boolean],
                            categories: Option[Seq[String]])

object SafetyAttributes {
  implicit val formats: OFormat[SafetyAttributes] = Json.format[SafetyAttributes]
}

case class Prediction(content: String,
                      safetyAttributes: Option[SafetyAttributes] = None)

object Prediction {
  implicit val formats: OFormat[Prediction] = Json.format[Prediction]
}
