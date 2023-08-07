/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models

import play.api.libs.json.{Json, Reads, Writes}

case class GCPRequest(product_id: String,
                      projectId: String,
                      parameters: Option[Parameters] = None,
                      datasetSize: Option[Int] = None,
                      filters: Seq[String] = Seq.empty,
                     )

object GCPRequest {
  implicit val reads: Reads[GCPRequest] = Json.using[Json.WithDefaultValues].reads[GCPRequest]
  implicit val writes: Writes[GCPRequest] = Json.writes[GCPRequest]
}
