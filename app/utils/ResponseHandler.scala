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

package utils

import com.fasterxml.jackson.core.JsonParseException
import com.google.cloud.aiplatform.v1beta1._
import models._
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json

import scala.annotation.tailrec

object ResponseHandler extends Logging {

  def sanitiseOutput(output: String): String = output.replaceAll("\n", "").replaceAll("\\*", "")

  def responseHandling(response: PredictResponse): Either[GCPErrorResponse, SentimentAnalysisResponse] = {
    val prediction = response.getPredictionsList.get(0)

    val values = Seq(
      "struct_value",
      "fields",
      "key:",
      "string_value",
      "bool_value",
      "null_value",
      "number_value",
      "values",
      "list_value"
    )

    @tailrec
    def outputSanitization(input: String, valuesToUpdate: Seq[String] = Seq.empty): String = {
      valuesToUpdate match {
        case strHead :: strTail =>
          outputSanitization(
            input.replaceAll(strHead, s"""~99~${strHead.replaceAll(":", "")}~99~:"""),
            strTail)
        case _ => input
      }
    }

    val newOutput = outputSanitization(s"{${prediction.toString}}", values)
    val fields = newOutput.split("""~99~fields~99~: \{""")

    def findField(key: String): Option[String] = fields.find(_.contains(s"""~99~key~99~: "$key""""))

    val content = findField("content")
    val blocked = findField("blocked")

    def sanitizedFieldContent(field: String, key: String): String = {
      val firstPass = "{" + (
        field
          .replaceAll("::", ":")
          .replaceAll(s"""~99~key~99~: "$key"""", s"""~99~key~99~: ~99~$key~99~,""")
          .replaceAll("value \\{", """~99~value~99~: {""")
          .replaceAll("~99~string_value~99~: \"", """~99~string_value~99~: ~99~""")
          .replaceAll("\\\\n", "~00~")
          .replaceAll("~00~~00~", " ")
          .replaceAll("~00~", "")
          .replaceAll("\\\\", "")
          .replaceAll("}", "")
        ) + "}}"

      firstPass
        .patch(firstPass.lastIndexOf("\""), "~99~", 1)
        .replaceAll("\"", """'""")
        .replaceAll("~99~", """"""")
    }

    lazy val blockedValue = {
      try {
        blocked.exists { _blocked =>
          val json = Json.parse(sanitizedFieldContent(_blocked, "blocked"))
          json.validate[PredictionField].asOpt match {
            case Some(PredictionField(_, PredictionFieldValue(Some(bool), _))) => bool
            case _ => false
          }
        }
      } catch {
        case ex@(_: JsonParseException) =>
          false
      }
    }

    content match {
      case Some(content) if content.nonEmpty && !blockedValue =>
        val contentJson = Json.parse(sanitizedFieldContent(content, "content"))

        contentJson.validate[PredictionField].asEither.left.map(_ => GCPErrorResponse(BAD_REQUEST, "Could not map content to PredictionField")).map {
          case PredictionField(_, PredictionFieldValue(_, Some(stringContent))) =>

            val sanitisedContent = sanitiseOutput(stringContent)

            logger.debug(s"[ResponseHandler][responseHandling] Sanitized API content. $sanitisedContent")
            SentimentAnalysisResponse(
              Seq(Prediction(
                content = sanitisedContent, safetyAttributes = Some(SafetyAttributes(
                  Some(Seq.empty), blocked = Some(blockedValue), Some(Seq.empty)
                ))
              ))
            )
        }
      case _ =>
        if (blockedValue) {
          logger.error(s"[ResponseHandler][responseHandling] GCP Request content was blocked")
          Left(GCPErrorResponse(BAD_REQUEST, "GCP Request was blocked"))
        } else {
          logger.info(s"[ResponseHandler][responseHandling] GCP Request did not return content")
          Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, "GCP Request did not return content"))
        }
    }
  }
}
