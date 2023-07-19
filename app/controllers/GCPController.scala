/*
 * Copyright 2023 Innové Gen AI
 *
 */

package controllers

import actions.RequestBodyAction.RequestBodyActionBuilder
import models.{GCPFreeformRequest, GCPRequest, PredictionOutput}
import play.api.libs.json.Json
import play.api.mvc._
import services.GCPService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GCPController @Inject()(gcpService: GCPService)
                             (implicit val controllerComponents: ControllerComponents, ec: ExecutionContext) extends BaseController {

  private def toResult(predictionOutput: Option[PredictionOutput]): Result = {
    predictionOutput match {
      case Some(predictionOutput) => Ok(Json.toJson(predictionOutput))
      case None => NoContent
    }
  }

  def callSentimentAnalysis(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        gcpService.callSentimentAnalysis(gcloudAccessToken, request.body).map {
        case Left(error) => Status(error.status)(error.response)
        case Right(response) => toResult(response)
      }
      case None => Future.successful(Unauthorized)
    }
  }

  def callSummariseInputs(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        gcpService.callSummariseInputs(gcloudAccessToken, request.body).map {
        case Left(error) => Status(error.status)(error.response)
        case Right(response) => toResult(response)
      }
      case None => Future.successful(Unauthorized)
    }
  }

  def callGetKeywords(): Action[GCPRequest] = Action.validateBodyAs[GCPRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        gcpService.callGetKeywords(gcloudAccessToken, request.body).map {
        case Left(error) => Status(error.status)(error.response)
        case Right(response) => toResult(response)
      }
      case None => Future.successful(Unauthorized)
    }
  }

  def callFreeform(): Action[GCPFreeformRequest] = Action.validateBodyAs[GCPFreeformRequest].async { implicit request =>
    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        gcpService.callFreeform(gcloudAccessToken, request.body).map {
        case Left(error) => Status(error.status)(error.response)
        case Right(response) => toResult(response)
      }
      case None => Future.successful(Unauthorized)
    }
  }
}
