/*
 * Copyright 2023 Innové Gen AI
 *
 */

package controllers

import mocks.MockGCPService
import models.{GCPRequest, Prediction, SentimentAnalysisResponse}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class GCPControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockGCPService {

  "GCPController GET" should {
    "call the service with a sentiment analysis response" in {

      val request = GCPRequest(inputs = Seq(
        "It was great"
      ))

      mockCallSentimentAnalysis(
        request,
        Right(SentimentAnalysisResponse(
          Seq(
            Prediction("positive")
          )
        )))

      def fakeRequest(body: GCPRequest): FakeRequest[GCPRequest] = FakeRequest("POST", "/").withBody(body)

      val controller = new GCPController(mockGCPService)(stubControllerComponents(), scala.concurrent.ExecutionContext.Implicits.global)
      val home = controller.callSentimentAnalysis().apply(fakeRequest(request).withHeaders(AUTHORIZATION -> "Bearer token"))

      status(home) mustBe OK
      contentAsString(home) mustBe """{"predictions":[{"content":"positive"}]}"""
    }
  }
}
