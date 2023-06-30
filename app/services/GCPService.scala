/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services

import connector.GCPConnector
import models.{GCPErrorResponse, SentimentAnalysisResponse}

import javax.inject._
import scala.concurrent.Future

@Singleton
class GCPService @Inject()(gcpConnector: GCPConnector) {

  def callSentimentAnalysis(gcloudAccessToken: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callSentimentAnalysis(gcloudAccessToken)
  }
}
