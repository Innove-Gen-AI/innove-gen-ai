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
