/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services

import connector.GCPConnector
import models.{GCPErrorResponse, GCPFreeformRequest, GCPRequest, SentimentAnalysisResponse}

import javax.inject._
import scala.concurrent.Future

@Singleton
class GCPService @Inject()(gcpConnector: GCPConnector) {

  def callSentimentAnalysis(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callSentimentAnalysis(gcloudAccessToken, request.inputs, request.parameters)
  }

  def callGetKeywords(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callGetKeywords(gcloudAccessToken, request.inputs, request.parameters)
  }

  def callSummariseInputs(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callSummariseInputs(gcloudAccessToken, request.inputs, request.parameters)
  }

  def callFreeform(gcloudAccessToken: String, request: GCPFreeformRequest): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callFreeform(gcloudAccessToken, request.inputs, request.prompt, request.parameters)
  }
}
