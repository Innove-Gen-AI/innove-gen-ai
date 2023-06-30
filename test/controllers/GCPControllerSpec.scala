/*
 * Copyright 2023 Innové Gen AI
 *
 */

package controllers

import mocks.MockGCPService
import models.{Prediction, SentimentAnalysisResponse}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class GCPControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockGCPService {

  "GCPController GET" should {
    "call the service with a sentiment analysis response" in {

      mockCallSentimentAnalysis(Right(SentimentAnalysisResponse(
        Seq(
          Prediction("positive")
        )
      )))

      val controller = new GCPController(stubControllerComponents(), mockGCPService)(scala.concurrent.ExecutionContext.Implicits.global)
      val home = controller.callSentimentAnalysis().apply(FakeRequest(GET, "/").withHeaders(AUTHORIZATION -> "Bearer token"))

      status(home) mustBe OK
      contentAsString(home) mustBe """{"predictions":[{"content":"positive"}]}"""
    }
  }
}
