/*
 * Copyright 2023 Innové Gen AI
 *
 */

package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.GCPService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GCPController @Inject()(val controllerComponents: ControllerComponents,
                              gcpService: GCPService)
                             (implicit ec: ExecutionContext) extends BaseController {

  def callSentimentAnalysis(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>

    request.headers.get(AUTHORIZATION) match {
      case Some(gcloudAccessToken) =>
        gcpService.callSentimentAnalysis(gcloudAccessToken).map {
        case Left(error) => Status(error.status)(error.response)
        case Right(response) => Ok(Json.toJson(response))
      }
      case None => Future.successful(Unauthorized)
    }
  }
}
