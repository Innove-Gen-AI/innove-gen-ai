/*
 * Copyright 2023 Innové Gen AI
 *
 */

package actions

import play.api.libs.json.Reads
import play.api.mvc.Results.BadRequest
import play.api.mvc._

import scala.concurrent.ExecutionContext

object RequestBodyAction extends RequestBodyActionImplicits

trait RequestBodyActionImplicits {

  implicit class RequestBodyActionBuilder[Body](actionBuilder: ActionBuilder[Request, Body])(implicit cc: ControllerComponents) {

    def validateBodyAs[Model](implicit executionContext: ExecutionContext, reads: Reads[Model]): ActionBuilder[Request, Model] = {
      actionBuilder(cc.parsers.default.validate { body =>
        body.asJson match {
          case Some(json) => json.validate[Model].asEither.left.map(_ => BadRequest)
          case None => Left(BadRequest)
        }
      })
    }
  }
}
