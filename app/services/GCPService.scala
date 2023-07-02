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

  val inputs: Seq[String] = Seq(
    "This fits in perfectly. The portability is of course the selling point. The battery isnt quite good enough to last a day/night with any kind of brightness however it looks great - As expected from Hue and the price tag.",
    "Great light but the app plays up and a little disappointed I had to buy a second one as first one stopped working after a few years.",
    "Good overall but the Bluetooth connection can be unreliable at times. Light and features are excellent when connected but the experience is let down by the app",
    "The device in the image is not the device in the description. I was not very happy. Especially when it was a 1 day sale and could not just order the correct item again at the reduced cost.",
    "Too expensive and not value for money",
  )

  def callSentimentAnalysis(gcloudAccessToken: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callSentimentAnalysis(gcloudAccessToken, inputs)
  }

  def callGetKeywords(gcloudAccessToken: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callGetKeywords(gcloudAccessToken, inputs)
  }

  def callSummariseInputs(gcloudAccessToken: String): Future[Either[GCPErrorResponse, SentimentAnalysisResponse]] = {
    gcpConnector.callSummariseInputs(gcloudAccessToken, inputs)
  }
}
