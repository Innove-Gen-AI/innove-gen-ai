/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services

import connector.GCPConnector
import models.{GCPErrorResponse, GCPFreeformRequest, GCPRequest, SentimentAnalysisResponse}
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

  def callSentimentAnalysis(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(30), request.filters).flatMap {
      inputs =>
        gcpConnector.callSentimentAnalysis(gcloudAccessToken, inputs, request.parameters)
    }
  }

  def callGetKeywords(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(50), request.filters).flatMap {
      inputs =>
        gcpConnector.callGetKeywords(gcloudAccessToken, inputs, request.parameters)
    }
  }

  def callSummariseInputs(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(100), request.filters).flatMap {
      inputs =>
        gcpConnector.callSummariseInputs(gcloudAccessToken, inputs, request.parameters)
    }
  }

  def callFreeform(gcloudAccessToken: String, request: GCPFreeformRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    datasetInputs(request.product_id, request.datasetSize.getOrElse(100), request.filters).flatMap {
      inputs =>
        gcpConnector.callFreeform(gcloudAccessToken, inputs, request.prompt, request.parameters)
    }
  }
}
