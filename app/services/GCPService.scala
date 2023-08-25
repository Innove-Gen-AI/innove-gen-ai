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

package services

import config.AppConfig
import connector.GCPConnector
import models.{GCPBaseRequest, GCPErrorResponse, GCPFreeformRequest, GCPRequest, PredictionOutput, SafetyAttributes, SentimentAnalysisResponse}
import play.api.Logging

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GCPService @Inject()(gcpConnector: GCPConnector,
                           productService: ProductService,
                           appConfig: AppConfig)
                          (implicit ec: ExecutionContext) extends Logging {

  private def datasetInputs(productId: String, datasetSize: Int, filters: Seq[String])
                           (implicit method: String): Future[Seq[String]] = {
    productService.getProductReviews(productId, filters, method).map {
      reviews =>
        reviews.take(datasetSize)
    }
  }

  private def batchHandling(baseRequest: GCPBaseRequest,
                            gcpFunction: GCPBaseRequest => Future[Either[GCPErrorResponse, SentimentAnalysisResponse]],
                            toSinglePrediction: Seq[PredictionOutput] => PredictionOutput,
                            batchSize: Int = 20)
                           (implicit method: String): Future[Either[GCPErrorResponse, PredictionOutput]] = {

    val t1 = System.nanoTime()

    Future.sequence(baseRequest.inputs.grouped(batchSize).map { batch =>

      logger.info(s"[GCPService][batchHandling][$method] Inputs size: ${baseRequest.inputs.size}, batch size: ${batch.size}")

      if (batch.nonEmpty) {
        gcpFunction(baseRequest.copy(inputs = batch)).map {
          case Left(error) => Some(Left(error))
          case Right(SentimentAnalysisResponse(predictions)) =>
            Some(Right(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes)))
        }
      } else {
        Future.successful(None)
      }
    }).map(_.flatten.toSeq).flatMap { responses =>

      val result = responses.filter(response => response.isRight)
      val duration = (System.nanoTime - t1) / 1e9d

      logger.info(s"[GCPService][batchHandling][$method] Batch handling duration: $duration. Success responses: ${responses.count(_.isRight)} from ${responses.size} requests.")

      if (result.nonEmpty) {
        Future.successful(Right(toSinglePrediction(result.flatMap(_.toSeq))))
      } else {

        val finalBatch = scala.util.Random.shuffle(baseRequest.inputs).take(5)

        //final backup if blocked
        logger.warn(s"[GCPService][batchHandling][$method] Attempting final backup call")
        gcpFunction(baseRequest.copy(inputs = finalBatch)).map {
          case Right(SentimentAnalysisResponse(predictions)) =>
            logger.warn(s"[GCPService][batchHandling][$method] Final backup call successful")
            Right(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes))
          case Left(error) =>
            logger.error(s"[GCPService][batchHandling][$method] Final backup call failed. Error: $error")
            Left(error)
        }
      }
    }
  }

  private def batchContentToSingleOutput(batchOutput: Seq[PredictionOutput]): PredictionOutput = {
    val allContent = batchOutput.map(_.content)
    val content = s"[${allContent.map(_.replaceAll("\\[", "").replaceAll("]","")).mkString(", ")}]"
    PredictionOutput(content, safetyAttributes = batchOutput.head.safetyAttributes, title = batchOutput.head.title)
  }

  private def batchContentToSingleOutputDistinct(batchOutput: Seq[PredictionOutput]): PredictionOutput = {
    val allContent = batchOutput.map(_.content)
    val list: Seq[String] = allContent.map(_.replaceAll("\\[", "").replaceAll("]","")).mkString(",").replaceAll(", ",",").split(",").distinct
    val content = s"[${scala.util.Random.shuffle(list).take(9).mkString(", ")}]"
    PredictionOutput(content, safetyAttributes = batchOutput.head.safetyAttributes, title = batchOutput.head.title)
  }

  def callSentimentAnalysis(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, PredictionOutput]] = {
    implicit val method: String = "callSentimentAnalysis"
    datasetInputs(request.product_id, request.datasetSize.getOrElse(30), request.filters).flatMap {
      inputs =>
        batchHandling(GCPBaseRequest(gcloudAccessToken, inputs, request.projectId, request.parameters, None), gcpConnector.callSentimentAnalysis, batchContentToSingleOutput, batchSize = 17)
    }
  }

  def callGetKeywords(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, PredictionOutput]] = {
    implicit val method: String = "callGetKeywords"
    datasetInputs(request.product_id, request.datasetSize.getOrElse(50), request.filters).flatMap {
      inputs =>
        batchHandling(GCPBaseRequest(gcloudAccessToken, inputs, request.projectId, request.parameters, None), gcpConnector.callGetKeywords, batchContentToSingleOutputDistinct)
    }
  }

  def callSummariseInputs(gcloudAccessToken: String, request: GCPRequest): Future[Either[GCPErrorResponse, PredictionOutput]] = {
    implicit val method: String = "callSummariseInputs"
    datasetInputs(request.product_id, request.datasetSize.getOrElse(100), request.filters).flatMap {
      inputs =>
        gcpConnector.callSummariseInputs(gcloudAccessToken, inputs, request.projectId, request.parameters).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(SentimentAnalysisResponse(predictions)) =>
            gcpConnector.callGenerateTitle(gcloudAccessToken, inputs, request.projectId).map { title =>
              Right(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes, title))
            }
        }
    }
  }

  def callFreeform(gcloudAccessToken: String, request: GCPFreeformRequest): Future[Either[GCPErrorResponse, PredictionOutput]] = {
    implicit val method: String = "callFreeform"
    datasetInputs(request.product_id, request.datasetSize.getOrElse(100), request.filters).flatMap {
      inputs =>

        val title: Future[String] = {
          val t1 = System.nanoTime()
          gcpConnector.callGenerateTitle(gcloudAccessToken, scala.util.Random.shuffle(inputs).take(20), request.projectId).map { titleResult =>
            val duration = (System.nanoTime - t1) / 1e9d
            logger.info(s"[GCPService][$method][callGenerateTitle] callGenerateTitle duration: $duration")
            titleResult
          }
        }

        val t1 = System.nanoTime()

        gcpConnector.callFreeform(GCPBaseRequest(gcloudAccessToken, inputs, request.projectId, request.parameters, Some(request.prompt))).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(SentimentAnalysisResponse(predictions)) =>
            title.map { title =>
              Right(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes, title))
            }
        }.flatMap {
          finalResult =>
            val finalTimeNow = System.nanoTime
            val finalDuration = (finalTimeNow - t1) / 1e9d
            logger.info(s"[GCPService][$method] Non batched review duration: $finalDuration for ${inputs.size} reviews")

            def finalBackup(): Future[Either[GCPErrorResponse, PredictionOutput]] = {

              val lastBatch = scala.util.Random.shuffle(inputs).take(12)

              logger.warn(s"[GCPService][freeformHandling][$method] Attempting final backup call")
              gcpConnector.callFreeform(GCPBaseRequest(gcloudAccessToken, lastBatch, request.projectId, request.parameters, Some(request.prompt))).flatMap {
                case Right(SentimentAnalysisResponse(predictions)) =>
                  logger.info(s"[GCPService][freeformHandling][$method] Final backup call successful")
                  title.map{ title =>
                    Right(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes, title))
                  }
                case Left(error) =>
                  logger.error(s"[GCPService][freeformHandling][$method] Final backup call failed. Resorting to local review. Error: $error")
                  Future.successful(Right(PredictionOutput(lastBatch.head, safetyAttributes = Some(SafetyAttributes(
                    Some(Seq.empty), blocked = Some(false), Some(Seq.empty)
                  )))))
              }
            }

            finalResult match {
              case Right(finalResult) => Future.successful(Right(finalResult))
              case Left(error) =>
                logger.error(s"[GCPService][$method] Error Response - ${error}")
                finalBackup()
            }
        }
    }
  }

  private def reviewBatchHandling(baseRequest: GCPBaseRequest,
                                  gcpFunction: GCPBaseRequest => Future[Either[GCPErrorResponse, SentimentAnalysisResponse]],
                                  batchSize: Int = 80)
                                 (implicit method: String): Future[Either[GCPErrorResponse, Option[PredictionOutput]]] = {

    val t1 = System.nanoTime()

    Future.sequence(baseRequest.inputs.grouped(batchSize).map { batch =>
      logger.info(s"[GCPService][reviewBatchHandling][$method] Inputs size: ${baseRequest.inputs.size}, batch size: ${batch.size}")

      if (batch.nonEmpty) {
        gcpFunction(baseRequest.copy(inputs = batch)).map {
          case Left(error) => Some(Left(error))
          case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty && predictions.head.content.nonEmpty =>
            Some(Right(Some(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes))))
          case Right(_) => Some(Right(None))
        }
      } else {
        Future.successful(None)
      }
    }).map(_.flatten.toSeq).flatMap { responses =>

      val result = responses.filter(response => response.isRight && response.exists(_.isDefined))

      val duration = (System.nanoTime - t1) / 1e9d

      logger.info(s"[GCPService][reviewBatchHandling][$method] Review batch handling duration: $duration. Success responses: ${responses.map(_.isRight).size} from ${responses.size} requests.")

      if (result.nonEmpty && result.size > 1) {

        val inputs = result.flatMap(_.toSeq).map(_.get).map(_.content)

        println(s"$method final inputs size = " + inputs.size)

        val finalT1 = System.nanoTime()

        gcpFunction(baseRequest.copy(inputs = inputs)).map {
          case Right(SentimentAnalysisResponse(predictions)) if predictions.nonEmpty && predictions.head.content.nonEmpty =>
            Right(Some(PredictionOutput(predictions.head.content, predictions.head.safetyAttributes)))
          case _ =>
            val backupResult = result.flatMap(_.toSeq).map(_.get).head
            Right(Some(PredictionOutput(backupResult.content, backupResult.safetyAttributes)))
        }.map { finalResult =>
          val finalTimeNow = System.nanoTime
          val finalDuration = (finalTimeNow - finalT1) / 1e9d
          logger.info(s"[GCPService][reviewBatchHandling][$method] Final review batch handling duration: $finalDuration.")
          finalResult
        }
      } else {
        val finalDuration = (System.nanoTime - t1) / 1e9d
        logger.info(s"[GCPService][reviewBatchHandling][$method] Single item in batch duration: $finalDuration.")
        Future.successful(responses.head)
      }
    }
  }
}
