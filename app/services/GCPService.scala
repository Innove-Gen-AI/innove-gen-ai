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

package services

import connector.GCPConnector
import models.{GCPErrorResponse, GCPFreeformRequest, GCPRequest, PredictionOutput, SentimentAnalysisResponse}
import play.api.Logging

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GCPService @Inject()(gcpConnector: GCPConnector,
                           productService: ProductService)
                          (implicit ec: ExecutionContext) extends Logging {

  private def datasetInputs(productId: String, datasetSize: Int, filters: Seq[String]): Future[Seq[String]] = {
    productService.getProductReviews(productId, filters).map {
      reviews =>
        reviews.map(_.review_text).take(datasetSize)
    }
  }

  def callSentimentAnalysis(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, Option[PredictionOutput]]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(30), request.filters).flatMap {
      inputs =>
        gcpConnector.callSentimentAnalysis(gcloudAccessToken, inputs, request.projectId, request.parameters).map {
          case Left(error) => Left(error)
          case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty =>
            Right(Some(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes)))
          case Right(_) => Right(None)
        }
    }
  }

  def callGetKeywords(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, Option[PredictionOutput]]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(50), request.filters).flatMap {
      inputs =>
        gcpConnector.callGetKeywords(gcloudAccessToken, inputs, request.projectId, request.parameters).map{
          case Left(error) => Left(error)
          case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty =>
            Right(Some(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes)))
          case Right(_) => Right(None)
        }
    }
  }

  def callSummariseInputs(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, Option[PredictionOutput]]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(100), request.filters).flatMap {
      inputs =>
        gcpConnector.callSummariseInputs(gcloudAccessToken, inputs, request.projectId, request.parameters).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty =>
            gcpConnector.callGenerateTitle(gcloudAccessToken, inputs, request.projectId).map { title =>
              Right(Some(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes, title)))
            }
          case Right(_) =>  Future.successful(Right(None))
        }
    }
  }

  def callFreeform(gcloudAccessToken: String, request: GCPFreeformRequest): Future[Either[GCPErrorResponse, Option[PredictionOutput]]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(100), request.filters).flatMap {
      inputs =>
        gcpConnector.callFreeform(gcloudAccessToken, inputs, request.prompt, request.projectId, request.parameters).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty =>
            gcpConnector.callGenerateTitle(gcloudAccessToken, inputs, request.projectId).map { title =>
              Right(Some(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes, title)))
            }
          case Right(_) =>  Future.successful(Right(None))
        }
    }
  }
}
