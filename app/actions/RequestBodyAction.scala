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

package actions

import play.api.Logging
import play.api.libs.json.Reads
import play.api.mvc.Results.BadRequest
import play.api.mvc._

import scala.concurrent.ExecutionContext

object RequestBodyAction extends RequestBodyActionImplicits

trait RequestBodyActionImplicits extends Logging {

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
