/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models

import play.api.libs.json.{Json, OFormat}

case class PredictionOutput(content: String,
                            safetyAttributes: Option[SafetyAttributes] = None,
                            title: String = "Overall sentiment")

object PredictionOutput {
  implicit val formats: OFormat[PredictionOutput] = Json.format[PredictionOutput]
}
