/*
 * Copyright 2023 Innové Gen AI
 *
 */

package models

import play.api.libs.json.{Json, Reads, Writes}

case class GCPFreeformRequest(product_id: String,
                              projectId: String,
                              parameters: Option[Parameters] = None,
                              datasetSize: Option[Int] = None,
                              filters: Seq[String] = Seq.empty,
                              prompt: String)

object GCPFreeformRequest {
  implicit val reads: Reads[GCPFreeformRequest] = Json.using[Json.WithDefaultValues].reads[GCPFreeformRequest]
  implicit val writes: Writes[GCPFreeformRequest] = Json.writes[GCPFreeformRequest]
}
