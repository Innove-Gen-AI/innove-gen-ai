/*
 * Copyright 2023 Innové Gen AI
 *
 */

package mocks

import models.{GCPErrorResponse, SentimentAnalysisResponse}
import org.scalamock.handlers.CallHandler1
import org.scalamock.scalatest.MockFactory
import services.GCPService

import scala.concurrent.Future

trait MockGCPService extends MockFactory {

  protected val mockGCPService: GCPService  = mock[GCPService]

  def mockCallSentimentAnalysis(response: Either[GCPErrorResponse, SentimentAnalysisResponse]): CallHandler1[String, Future[Either[GCPErrorResponse, SentimentAnalysisResponse]]] ={
    (mockGCPService.callSentimentAnalysis(_: String))
      .expects(*)
      .returning(Future.successful(response))
  }
}
