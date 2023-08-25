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

package controllers

import actions.RequestBodyAction.RequestBodyActionBuilder
import com.google.common.util.concurrent.RateLimiter
import models.{GCPErrorResponse, GCPFreeformRequest, GCPRequest, PredictionOutput, SafetyAttributes}
import play.api.Logging
import play.api.http.Status.{NOT_ACCEPTABLE, NO_CONTENT}
import play.api.libs.json.Json
import play.api.mvc._
import services.GCPService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future, blocking}


@Singleton
class GCPController @Inject()(gcpService: GCPService)
                             (implicit val controllerComponents: ControllerComponents, ec: ExecutionContext) extends BaseController with Logging {

  private def toResult(predictionOutput: PredictionOutput): Result = {
    val json = Json.toJson(predictionOutput)
    logger.debug(s"[GCPController][toResult] Return json = $json")
    Ok(json)
  }

  var currentOverclock = 0
  var recentPredictions: Seq[RecentPrediction] = Seq.empty

  case class RecentPrediction(method: String, request: GCPRequest, predictionOutput: PredictionOutput)

  def throttled(rateLimiter: RateLimiter, request: GCPRequest, method: String)(fut: => Future[Either[GCPErrorResponse, PredictionOutput]]): Future[Either[GCPErrorResponse, PredictionOutput]] = {
    implicit val ec: ExecutionContext = ExecutionContext.parasitic

    def recentPrediction: Option[(PredictionOutput, String)] = {
      recentPredictions.find(rp => rp.request == request && rp.method == method).map(recentPrediction => (recentPrediction.predictionOutput, recentPrediction.method))
    }

    if (rateLimiter.tryAcquire(1)) {
      logger.info("[GCPController][throttled] Request acquired")
      currentOverclock = 0
      recentPredictions = recentPredictions.filterNot(x => x.request == request && x.method == method)
    } else {
      logger.warn(s"[GCPController][throttled] Request rate limited. Added to overclock. $currentOverclock")
      currentOverclock = currentOverclock + 1
    }

    if (recentPrediction.isDefined && currentOverclock > 5) {
      logger.info(s"[GCPController][throttled] Found a recent prediction from same request. Method - ${recentPrediction.map(_._2)}")
      val foundPrediction = recentPrediction.get
      recentPredictions = recentPredictions.filterNot(x => x.request == request && x.method == method)
      Future.successful(Right(foundPrediction._1))
    } else {
      recentPredictions = recentPredictions.filterNot(x => x.request == request && x.method == method)

      logger.info(s"[GCPController][throttled] Recent predication for request not found. Doing future call..")
      Future(blocking(rateLimiter.acquire(1))).flatMap(_ => fut).map {
        case success@Right(predictionOutput: PredictionOutput) =>
          recentPredictions = recentPredictions ++ Seq(RecentPrediction(method, request, predictionOutput))
          success
        case error@_ =>
          logger.warn(s"[GCPController][throttled] Received error response. Checking recent predictions.")
          recentPrediction.map(_._1) match {
            case Some(value) =>
              logger.info(s"[GCPController][throttled] Found a recent prediction from same request. Method - ${recentPrediction.get._2}")
              Right(value)
            case None =>
              logger.warn(s"[GCPController][throttled] Recent predication for request not found.")
              error
          }
      }
    }
  }

  val rateLimiter: RateLimiter = RateLimiter.create(10)

  private lazy val errorBackupDefaultSentiment: PredictionOutput = PredictionOutput(
    content = "['neutral', 'neutral', 'neutral', 'neutral', 'negative', 'positive', 'positive']", safetyAttributes = Some(SafetyAttributes(
      Some(Seq.empty), blocked = Some(false), Some(Seq.empty)
    )),
  )

  private lazy val errorBackupDefaultKeywords: PredictionOutput = PredictionOutput(
    content = "['hydrating', 'lightweight', 'sensitive skin', 'fragrance']", safetyAttributes = Some(SafetyAttributes(
      Some(Seq.empty), blocked = Some(false), Some(Seq.empty)
    )),
  )

  def callSentimentAnalysis(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        throttled(rateLimiter, request.body, "callSentimentAnalysis") {
          gcpService.callSentimentAnalysis(gcloudAccessToken, request.body)
        }.map {
          case Right(response) => toResult(response)
          case Left(GCPErrorResponse(status, _)) if status == NOT_ACCEPTABLE || status == NO_CONTENT =>
            logger.warn(s"[GCPController][callSentimentAnalysis] Resorting to errorBackupDefaultSentiment")
            toResult(errorBackupDefaultSentiment)
          case Left(error) => Status(error.status)(error.response)
        }
      case None => Future.successful(Unauthorized)
    }
  }

  def callGetKeywords(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        throttled(rateLimiter, request.body, "callGetKeywords") {
          gcpService.callGetKeywords(gcloudAccessToken, request.body)
        }.map {
          case Right(response) => toResult(response)
          case Left(GCPErrorResponse(status, _)) if status == NOT_ACCEPTABLE || status == NO_CONTENT =>
            logger.warn(s"[GCPController][callGetKeywords] Resorting to errorBackupDefaultKeywords")
            toResult(errorBackupDefaultKeywords)
          case Left(error) => Status(error.status)(error.response)
        }
      case None => Future.successful(Unauthorized)
    }
  }

  def callFreeform(): Action[GCPFreeformRequest] = Action.validateBodyAs[GCPFreeformRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        throttled(rateLimiter, request.body.toGCPRequest, "callFreeform") {
          gcpService.callFreeform(gcloudAccessToken, request.body)
        }.map {
          case Left(error) => Status(error.status)(error.response)
          case Right(response) => toResult(response)
        }
      case None => Future.successful(Unauthorized)
    }
  }

  def callSummariseInputs(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        throttled(rateLimiter, request.body, "callSummariseInputs") {
          gcpService.callSummariseInputs(gcloudAccessToken, request.body)
        }.map {
          case Left(error) => Status(error.status)(error.response)
          case Right(response) => toResult(response)
        }
      case None => Future.successful(Unauthorized)
    }
  }
}
