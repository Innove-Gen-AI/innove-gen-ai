/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models

import play.api.libs.json.{Json, OFormat}

case class Instance(content: String)

object Instance {
  implicit val formats: OFormat[Instance] = Json.format[Instance]
}

case class Parameters(temperature: BigDecimal,
                      maxOutputTokens: Int,
                      topP: BigDecimal,
                      topK: BigDecimal)

object Parameters {
  implicit val formats: OFormat[Parameters] = Json.format[Parameters]
}

case class GCPPredictRequest(instances: Seq[Instance],
                             parameters: Parameters)

object GCPPredictRequest {
  implicit val formats: OFormat[GCPPredictRequest] = Json.format[GCPPredictRequest]
}

