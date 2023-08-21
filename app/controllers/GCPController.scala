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
import models.{GCPErrorResponse, GCPFreeformRequest, GCPRequest, PredictionOutput}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import services.GCPService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future, blocking}


@Singleton
class GCPController @Inject()(gcpService: GCPService)
                             (implicit val controllerComponents: ControllerComponents, ec: ExecutionContext) extends BaseController with Logging {

  private def toResult(predictionOutput: Option[PredictionOutput]): Result = {
    predictionOutput match {
      case Some(predictionOutput) => Ok(Json.toJson(predictionOutput))
      case None => NoContent
    }
  }

  var currentOverclock = 0
  var recentPredictions: Seq[RecentPrediction] = Seq.empty

  case class RecentPrediction(method: String, request: GCPRequest, predictionOutput: PredictionOutput)

  def throttled(rateLimiter: RateLimiter, request: GCPRequest, method: String)(fut: => Future[Either[GCPErrorResponse, Option[PredictionOutput]]]): Future[Either[GCPErrorResponse, Option[PredictionOutput]]] = {
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

    if (currentOverclock > 10) {
      logger.info(s"[GCPController][throttled] Request rate limit reached max overclock. Checking if successful recent prediction..")
      Future.successful(Right(recentPrediction.map(_._1)))
    } else {
      if(recentPrediction.isDefined){
        logger.info(s"[GCPController][throttled] Found a recent prediction from same request. Method - ${recentPrediction.map(_._2)}")
        recentPredictions = recentPredictions.filterNot(x => x.request == request && x.method == method)
        Future.successful(Right(recentPrediction.map(_._1)))
      } else {
        recentPredictions = recentPredictions.filterNot(x => x.request == request && x.method == method)

        logger.info(s"[GCPController][throttled] Recent predication for request not found. Doing future call..")
        Future(blocking(rateLimiter.acquire(1))).flatMap(_ => fut).map {
          case success@Right(Some(predictionOutput: PredictionOutput)) =>
            recentPredictions = recentPredictions ++ Seq(RecentPrediction(method, request, predictionOutput))
            success
          case error@_ =>
            logger.warn(s"[GCPController][throttled] Received error response. Checking recent predictions.")
            recentPrediction.map(_._1) match {
              case Some(value) =>
                logger.info(s"[GCPController][throttled] Found a recent prediction from same request. Method - ${recentPrediction.get._2}")
                Right(Some(value))
              case None =>
                logger.warn(s"[GCPController][throttled] Recent predication for request not found.")
                error
            }
        }
      }
    }
  }

  val rateLimiter: RateLimiter = RateLimiter.create(20)

  def callSentimentAnalysis(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        throttled(rateLimiter, request.body, "callSentimentAnalysis") {
          gcpService.callSentimentAnalysis(gcloudAccessToken, request.body)
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

  def callGetKeywords(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        throttled(rateLimiter, request.body, "callGetKeywords") {
          gcpService.callGetKeywords(gcloudAccessToken, request.body)
        }.map {
          case Left(error) => Status(error.status)(error.response)
          case Right(response) => toResult(response)
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
}
