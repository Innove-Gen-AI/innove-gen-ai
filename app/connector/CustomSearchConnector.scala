/*
 * Copyright 2020 HM Revenue & Customs
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
 */

package connector

import config.AppConfig
import models.GCPErrorResponse
import models.search.{CustomSearchResponse, Item}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSResponse

import java.net.URLEncoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CustomSearchConnector @Inject()(httpClient: WSClient,
                                      appConfig: AppConfig)
                                     (implicit ec: ExecutionContext) extends Logging {

  def callCustomSearchAPI(product_name: String,
                          brand_name: String,
                          cx: String,
                          api: String): Future[Either[GCPErrorResponse, CustomSearchResponse]] = {

    def encode(str: String): String = URLEncoder.encode(str, "UTF-8")

    val encodedQuery = encode(s"$brand_name $product_name")

    val url = s"https://customsearch.googleapis.com/customsearch/v1?cx=$cx&fileType=jpg&q=$encodedQuery&searchType=image&key=$api"

    Try {
      httpClient.url(url).get().map {
        case AhcWSResponse(underlying) =>
          underlying.status match {
            case OK =>
              Json.parse(underlying.body).asOpt[CustomSearchResponse] match {
                case Some(value) =>
                  logger.info(s"[CustomSearchConnector] Received success response from API. $brand_name $product_name ${value.items.headOption.map(_.link)}")
                  Right(value)
                case None =>
                  logger.error(s"[CustomSearchConnector] Error. Could not map response: ${underlying.body}")
                  Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, s"Error. Could not map response: ${underlying.body}"))
              }
            case status =>
              logger.error(s"[CustomSearchConnector] Error response. $status ${underlying.body}")
              Left(GCPErrorResponse(status = status, response = underlying.body))
          }
      }
    }.toEither match {
      case Left(exception) =>
        logger.error(s"[CustomSearchConnector] Exception. ${exception.getMessage}")
        Future.successful(Left(GCPErrorResponse(INTERNAL_SERVER_ERROR, exception.getMessage)))
      case Right(response) => response
    }
  }
}
