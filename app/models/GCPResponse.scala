/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models

import play.api.libs.json.{Json, OFormat}

trait GCPResponse

case class GCPErrorResponse(status: Int, response: String)

object GCPErrorResponse {
  implicit val formats: OFormat[GCPErrorResponse] = Json.format[GCPErrorResponse]
}
