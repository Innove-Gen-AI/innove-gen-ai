/*
 * Copyright 2023 Innové Gen AI
 *
 */

package connector

import models.{GCPErrorResponse, GCPPredictRequest, Instance, Parameters, SentimentAnalysisResponse}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GCPConnector @Inject()(httpClient: WSClient)
                            (implicit ec: ExecutionContext) extends Logging {

  private def sanitiseOutput(output: String): String = output.replaceAll("\n", "").replaceAll("\\*", "")

  def callGCPAPI(gcloudAccessToken: String,
                              gcpPredictRequest: GCPPredictRequest,
                              model: String = "text-bison@001"): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {

    val API_ENDPOINT = "us-central1-aiplatform.googleapis.com"
    val PROJECT_ID = "gen-innove"
    val MODEL_ID = model

    val headers = Seq(
      "Content-Type" -> "application/json",
      "Authorization" -> s"$gcloudAccessToken",
    )

    val url = s"https://$API_ENDPOINT/v1/projects/$PROJECT_ID/locations/us-central1/publishers/google/models/$MODEL_ID:predict"
    val body = Json.toJson(gcpPredictRequest)

    val bytes: Long = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name).length

    logger.info(s"[GCPConnector][callGCPAPI] Request Content-length $bytes")

    Try {
      httpClient.url(url).withHttpHeaders(headers: _*).post(body).map {
        case AhcWSResponse(underlying) =>
          underlying.status match {
            case OK =>
              Json.parse(underlying.body).asOpt[SentimentAnalysisResponse] match {
                case Some(value) =>
                  logger.info("[GCPConnector] Received success response from API.")
                  Right(value.copy(predictions = value.predictions.map(prediction => prediction.copy(content = sanitiseOutput(prediction.content)))))
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

  def indexedInputs(inputs: Seq[String]): String = inputs.zipWithIndex.map(input => s"{Index ${input._2} :: ${input._1}}").mkString(", ")

  def callSentimentAnalysis(gcloudAccessToken: String, inputs: Seq[String], parameters: Option[Parameters] = None): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger.info("[GCPConnector][callSentimentAnalysis] Calling sentiment analysis API")

    val sentimentOutputLength = 5

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(inputs)}] Classify the sentiment of the inputs: Options: ['positive', 'neutral', 'negative'] Output Notes: output sentiments as an array of strings"
        )
      ),
      parameters.getOrElse(Parameters(
        temperature = 0.2,
        maxOutputTokens = 1 + inputs.length * sentimentOutputLength,
        topP = 0.8,
        topK = 1
      ))
    )

    callGCPAPI(gcloudAccessToken, request)
  }

  def callSummariseInputs(gcloudAccessToken: String, inputs: Seq[String], parameters: Option[Parameters] = None): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger.info("[GCPConnector][callSummariseInputs] Calling summarise API")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(inputs)}] Summarize the general sentiments, highlights, and drawbacks of the inputs: Output Notes: do not include break lines such as '\n'"
        )
      ),
      parameters.getOrElse(Parameters(
        temperature = 0.2,
        maxOutputTokens = 700,
        topP = 0.8,
        topK = 40
      ))
    )

    callGCPAPI(gcloudAccessToken, request)
  }

  def callGetKeywords(gcloudAccessToken: String, inputs: Seq[String], parameters: Option[Parameters] = None): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger.info("[GCPConnector][callGetKeywords] Calling keywords API")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(inputs)}] Generate some keywords or tags that are common themes of the inputs: Keywords: Output Notes: do not include break lines such as '\n', output keywords as an array of strings"
        )
      ),
      parameters.getOrElse(Parameters(
        temperature = 0.2,
        maxOutputTokens = 400,
        topP = 0.9,
        topK = 40
      ))
    )

    callGCPAPI(gcloudAccessToken, request)
  }

  def callFreeform(gcloudAccessToken: String, inputs: Seq[String], prompt: String, parameters: Option[Parameters] = None): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger.info(s"[GCPConnector][callFreeform] Calling free form API. Prompt: $prompt")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(inputs)}] $prompt"

        )
      ),
      parameters.getOrElse(
        Parameters(
          temperature = 0.2,
          maxOutputTokens = 600,
          topP = 0.8,
          topK = 40
        )
      )
    )

    callGCPAPI(gcloudAccessToken, request)
  }

  def callGenerateTitle(gcloudAccessToken: String, inputs: Seq[String], parameters: Option[Parameters] = None): Future[String] = {
    logger.info(s"[GCPConnector][callFreeform] Calling title generation API.")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(inputs)}] Generate a catchy review title based on the sentiment of the inputs."

        )
      ),
      parameters.getOrElse(
        Parameters(
          temperature = 0.2,
          maxOutputTokens = 20,
          topP = 0.95,
          topK = 40
        )
      )
    )

    callGCPAPI(gcloudAccessToken, request).map {
      case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty => predictions.head.content
      case _ => "Overall sentiment"
    }
  }
}

