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
