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

