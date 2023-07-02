/*
 * Copyright 2023 Innové Gen AI
 *
 */

package mocks

import models.{GCPErrorResponse, GCPRequest, SentimentAnalysisResponse}
import org.scalamock.handlers.CallHandler2
import org.scalamock.scalatest.MockFactory
import services.GCPService

import scala.concurrent.Future

trait MockGCPService extends MockFactory {

  protected val mockGCPService: GCPService  = mock[GCPService]

  def mockCallSentimentAnalysis(request: GCPRequest, response: Either[GCPErrorResponse, SentimentAnalysisResponse]): CallHandler2[String, GCPRequest, Future[Either[GCPErrorResponse, SentimentAnalysisResponse]]] ={
    (mockGCPService.callSentimentAnalysis(_: String, _: GCPRequest))
      .expects(*, request)
      .returning(Future.successful(response))
  }
}
