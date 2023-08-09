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

package models

import play.api.libs.json.{Json, Reads, Writes}

case class GCPFreeformRequest(product_id: String,
                              projectId: String = "gen-innove",
                              parameters: Option[Parameters] = None,
                              datasetSize: Option[Int] = None,
                              filters: Seq[String] = Seq.empty,
                              prompt: String)

object GCPFreeformRequest {
  implicit val reads: Reads[GCPFreeformRequest] = Json.using[Json.WithDefaultValues].reads[GCPFreeformRequest]
  implicit val writes: Writes[GCPFreeformRequest] = Json.writes[GCPFreeformRequest]
}
