/*
 * Copyright 2023 Innové Gen AI
 *
 */

package connector

import config.AppConfig
import models.{GCPErrorResponse, SentimentAnalysisResponse}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GCPConnector @Inject()(httpClient: WSClient)
                            (implicit ec: ExecutionContext) extends Logging {

  def callSentimentAnalysis(gcloudAccessToken: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {

//    val client = EndpointServiceClient.create()
//    val endpoint = "us-central1-aiplatform.googleapis.com"
//    val name = EndpointName.of("gen-innove", "us-central1", endpoint)
//    client.getEndpoint(name)

    val API_ENDPOINT = "us-central1-aiplatform.googleapis.com"
    val PROJECT_ID = "gen-innove"
    val MODEL_ID = "text-bison@001"

    val headers = Seq(
      "Content-Type" -> "application/json",
      "Authorization" -> s"$gcloudAccessToken",
    )

    val body = {
      """{
        |    "instances": [
        |        {
        |           "content": "input: Something surprised me about this movie - it was actually original. It was not the same old recycled crap that comes out of Hollywood every month. I saw this movie on video because I did not even know about it before I saw it at my local video store. If you see this movie available - rent it - you will not regret it. Classify the sentiment of the message:"
        |        }
        |    ],
        |    "parameters": {
        |        "temperature": 0.2,
        |        "maxOutputTokens": 5,
        |        "topP": 0.8,
        |        "topK": 1
        |    }
        |}""".stripMargin
    }

    val url = s"https://${API_ENDPOINT}/v1/projects/${PROJECT_ID}/locations/us-central1/publishers/google/models/${MODEL_ID}:predict"

    Try {
      httpClient.url(url).withHttpHeaders(headers: _*).post(body).map {
        case AhcWSResponse(underlying) =>
          underlying.status match {
            case OK =>
              Json.parse(underlying.body).asOpt[SentimentAnalysisResponse] match {
                case Some(value) =>
                  logger.info("[GCPConnector] Received success response from API.")
                  Right(value)
                case None =>
                  logger.error(s"[GCPConnector] Error. Could not map response: ${underlying.body}")
                  Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, s"Error. Could not map response: ${underlying.body}"))
              }
            case status =>
              logger.error(s"[GCPConnector] Error response. $status ${underlying.body}")
              Left(GCPErrorResponse(status = status, response = underlying.body))
          }
      }
    }.toEither match {
      case Left(exception) =>
        logger.error(s"[GCPConnector] Exception. ${exception.getMessage}")
        Future.successful(Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, exception.getMessage)))
      case Right(response) => response
    }
  }
}

