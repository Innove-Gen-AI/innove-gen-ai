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

package controllers

import mocks.MockGCPService
import models.{GCPRequest, Prediction, PredictionOutput, SentimentAnalysisResponse}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class GCPControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockGCPService {

  "GCPController GET" should {
    "call the service with a sentiment analysis response" in {

      val request = GCPRequest("product")

      mockCallSentimentAnalysis(
        request,
        Right(Some(PredictionOutput(
          content = "positive"
        ))))

      def fakeRequest(body: GCPRequest): FakeRequest[GCPRequest] = FakeRequest("POST", "/").withBody(body)

      val controller = new GCPController(mockGCPService)(stubControllerComponents(), scala.concurrent.ExecutionContext.Implicits.global)
      val home = controller.callSentimentAnalysis().apply(fakeRequest(request).withHeaders(AUTHORIZATION -> "Bearer token"))

      status(home) mustBe OK
      contentAsString(home) mustBe """{"content":"positive"}"""
    }
  }
}
