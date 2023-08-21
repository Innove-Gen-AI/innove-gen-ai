/*
 * Copyright 2020 Innové Gen AI
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

import play.api.libs.json.{Json, OFormat}

case class PredictionFieldValue(bool_value: Option[Boolean],
                                string_value: Option[String])

object PredictionFieldValue {
  implicit val formats: OFormat[PredictionFieldValue] = Json.format[PredictionFieldValue]
}

case class PredictionField(key: String, value: PredictionFieldValue)

object PredictionField {
  implicit val formats: OFormat[PredictionField] = Json.format[PredictionField]
}
