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

object GCPRequestDefaults {
  val inputs: Seq[String] = Seq(
    "This fits in perfectly. The portability is of course the selling point. The battery isnt quite good enough to last a day/night with any kind of brightness however it looks great - As expected from Hue and the price tag.",
    "Great light but the app plays up and a little disappointed I had to buy a second one as first one stopped working after a few years.",
    "Good overall but the Bluetooth connection can be unreliable at times. Light and features are excellent when connected but the experience is let down by the app",
    "The device in the image is not the device in the description. I was not very happy. Especially when it was a 1 day sale and could not just order the correct item again at the reduced cost.",
    "Too expensive and not value for money",
  )

}

case class GCPRequest(inputs: Seq[String] = GCPRequestDefaults.inputs,
                      parameters: Option[Parameters] = None)

object GCPRequest {
  implicit val reads: Reads[GCPRequest] = Json.using[Json.WithDefaultValues].reads[GCPRequest]
  implicit val writes: Writes[GCPRequest] = Json.writes[GCPRequest]
}

case class GCPFreeformRequest(inputs: Seq[String] = GCPRequestDefaults.inputs,
                              parameters: Option[Parameters] = None,
                              prompt: String)

object GCPFreeformRequest {
  implicit val reads: Reads[GCPFreeformRequest] = Json.using[Json.WithDefaultValues].reads[GCPFreeformRequest]
  implicit val writes: Writes[GCPFreeformRequest] = Json.writes[GCPFreeformRequest]
}
