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

package connector

import com.google.api.gax.rpc.ResourceExhaustedException
import com.google.cloud.aiplatform.v1beta1._
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import io.grpc.StatusRuntimeException
import models._
import org.threeten.bp.Duration
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_ACCEPTABLE, OK}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSResponse
import utils.FutureHelper
import utils.ResponseHandler.{responseHandling, sanitiseOutput}

import java.util
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters._
import scala.util.Try

class GCPConnector @Inject()(client: GCPClient,
                             httpClient: WSClient)(implicit ec: ExecutionContext) extends FutureHelper {

  private def callGCPAPIHttp(gcloudAccessToken: String,
                             gcpPredictRequest: GCPPredictRequest,
                             projectId: String,
                             method: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {

    val API_ENDPOINT = "us-central1-aiplatform.googleapis.com"
    val MODEL_ID = "text-bison@001"

    val headers = Seq(
      "Content-Type" -> "application/json",
      "Authorization" -> s"$gcloudAccessToken",
    )

    val url = s"https://$API_ENDPOINT/v1/projects/$projectId/locations/us-central1/publishers/google/models/$MODEL_ID:predict"
    val body = Json.toJson(gcpPredictRequest)

    val bytes: Long = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name).length

    logger.info(s"[GCPConnector][$method] Request Content-length $bytes")

    Try {
      httpClient.url(url).withHttpHeaders(headers: _*).post(body).map {
        case AhcWSResponse(underlying) =>
          underlying.status match {
            case OK =>
              Json.parse(underlying.body).asOpt[SentimentAnalysisResponse] match {
                case Some(value) =>
                  logger.info(s"[GCPConnector][$method] Received success response from API.")
                  Right(value.copy(predictions = value.predictions.map(prediction => prediction.copy(content = sanitiseOutput(prediction.content)))))
                case None =>
                  logger.error(s"[GCPConnector][[$method]] Error. Could not map response: ${underlying.body}")
                  Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, s"Error. Could not map response: ${underlying.body}"))
              }
            case status =>
              logger.error(s"[GCPConnector][$method] Error response. $status ${underlying.body}")
              Left(GCPErrorResponse(status = status, response = underlying.body))
          }
      }
    }.toEither match {
      case Left(exception) =>
        logger.error(s"[GCPConnector][$method] Exception. ${exception.getMessage}")
        Future.successful(Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, exception.getMessage)))
      case Right(response) => response
    }
  }

  def callGCPAPIClient(gcloudAccessToken: String,
                       gcpPredictRequest: GCPPredictRequest,
                       projectId: String,
                       method: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {

    implicit val _method: String = method

    val MODEL_ID = "text-bison@001"
    val location = s"us-central1"
    val publisher = s"google"

    try {
      Try {
        val endpointName: EndpointName = EndpointName.ofProjectLocationPublisherModelName(projectId, location, publisher, MODEL_ID)

        val instance = Json.toJson(gcpPredictRequest.instances.head).toString()
        val parameters = Json.toJson(gcpPredictRequest.parameters).toString()

        val instanceValue = Value.newBuilder
        JsonFormat.parser.merge(instance, instanceValue)
        val instances = new util.ArrayList[Value]
        instances.add(instanceValue.build)

        val parameterValueBuilder = Value.newBuilder
        JsonFormat.parser.merge(parameters, parameterValueBuilder)
        val parameterValue = parameterValueBuilder.build

        val request: PredictRequest = PredictRequest.newBuilder().setEndpoint(endpointName.toString).addAllInstances(instances).setParameters(parameterValue).build()

        import com.google.api.gax.grpc.GrpcCallContext

        val context = GrpcCallContext.createDefault.withTimeout(Duration.ofSeconds(15))

        client.predictionServiceClient.predictCallable.withDefaultCallContext(context).futureCall(request)

      }.toEither match {
        case Left(exception) =>
          logger.error(s"[GCPConnector][callGCPAPIClient][$method] Exception. ${exception.toString}")
          Future.successful(Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, exception.getMessage)))
        case Right(futurePrediction) => toCompletableFuture(futurePrediction).asScala.map(responseHandling)
      }
    } catch {
      case exception: ResourceExhaustedException =>
        logger.error(s"[GCPConnector][callGCPAPIClient][$method] Rete limit Exception. ${exception.toString}")
        Future.successful(Left(GCPErrorResponse(NOT_ACCEPTABLE, exception.getMessage)))
      case exception =>
        logger.error(s"[GCPConnector][callGCPAPIClient][$method] Unexpected Exception. ${exception.toString}")
        Future.successful(Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, exception.getMessage)))
    }
  }

  private def indexedInputs(inputs: Seq[String]): String = inputs.zipWithIndex.map(input => s"{Index ${input._2} :: ${input._1}}").mkString(", ")

  def callSentimentAnalysis(baseRequest: GCPBaseRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger. debug ("[GCPConnector][callSentimentAnalysis] Calling sentiment analysis API")

    val sentimentOutputLength = 5

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(baseRequest.inputs)}] Classify the sentiment of the inputs: Options: ['positive', 'neutral', 'negative'] Output Notes: output sentiments as an array of strings"
        )
      ),
      baseRequest.parameters.getOrElse(Parameters(
        temperature = 0.2,
        maxOutputTokens = 1 + baseRequest.inputs.length * sentimentOutputLength,
        topP = 0.8,
        topK = 1
      ))
    )

    callGCPAPIClient(baseRequest.gcloudAccessToken, request, baseRequest.projectId, "callSentimentAnalysis").map {
      response =>
        logger.debug(s"[GCPConnector][callSentimentAnalysis] ${response}")
        response
    }
  }

  def callSummariseInputs(gcloudAccessToken: String, inputs: Seq[String], projectId: String, parameters: Option[Parameters] = None): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger. debug ("[GCPConnector][callSummariseInputs] Calling summarise API")

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

    callGCPAPIClient(gcloudAccessToken, request, projectId, "callSummariseInputs")
  }

  def callGetKeywords(baseRequest: GCPBaseRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger. debug ("[GCPConnector][callGetKeywords] Calling keywords API")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(baseRequest.inputs)}] Generate 9 or less meaningful keywords or tags that are common themes of the inputs: Keywords: Output Notes: do not include break lines such as '\n', output keywords as an array of strings"
        )
      ),
      baseRequest.parameters.getOrElse(Parameters(
        temperature = 0.2,
        maxOutputTokens = 80,
        topP = 0.9,
        topK = 40
      ))
    )

    callGCPAPIClient(baseRequest.gcloudAccessToken, request, baseRequest.projectId, "callGetKeywords")
  }

  def callFreeform(baseRequest: GCPBaseRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    logger. debug (s"[GCPConnector][callFreeform] Calling free form API. Prompt: ${baseRequest.prompt.get}")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(baseRequest.inputs)}] ${baseRequest.prompt.get}"
        )
      ),
      baseRequest.parameters.getOrElse(
        Parameters(
          temperature = 0.2,
          maxOutputTokens = 600,
          topP = 0.8,
          topK = 40
        )
      )
    )

    callGCPAPIClient(baseRequest.gcloudAccessToken, request, baseRequest.projectId, "callFreeform")
  }

  def callGenerateTitle(gcloudAccessToken: String, inputs: Seq[String], projectId: String, parameters: Option[Parameters] = None): Future[String] = {
    logger. debug (s"[GCPConnector][callGenerateTitle] Calling title generation API.")

    val request = GCPPredictRequest(
      Seq(
        Instance(
          s"inputs: [${indexedInputs(inputs)}] Generate a catchy review title based on the sentiment of the inputs. In 9 word or less"

        )
      ),
      parameters.getOrElse(
        Parameters(
          temperature = 0.4,
          maxOutputTokens = 23,
          topP = 0.95,
          topK = 40
        )
      )
    )

    callGCPAPIClient(gcloudAccessToken, request, projectId, "callGenerateTitle").map {
      case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty && predictions.head.content.nonEmpty => predictions.head.content
      case _ => "Overall sentiment"
    }
  }

}

