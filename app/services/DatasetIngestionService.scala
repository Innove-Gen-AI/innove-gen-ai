/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services

import config.AppConfig
import models.dataset.{ProductInfo, ProductReview}
import org.mongodb.scala.result.InsertManyResult
import play.api.Logging
import repositories.{ProductInfoRepository, ProductReviewRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DatasetIngestionService @Inject()(productInfoRepository: ProductInfoRepository,
                                        productReviewsRepository: ProductReviewRepository,
                                        appConfig: AppConfig)
                                       (implicit ec: ExecutionContext) extends Logging {

  def ingestDatasetFiles(): Future[InsertManyResult] = {

    import purecsv.unsafe._

//    def ingestProductInfo: Future[InsertManyResult] = {
//      val products: List[ProductInfo] = CSVReader[ProductInfo].readCSVFromFileName(appConfig.productsFilepath)
//
//      logger.info("[DatasetIngestionService][ingestProductInfo] Inserting data from dataset")
//      logger.info(s"[DatasetIngestionService][ingestProductInfo] Dataset size - ${products.size}")
//
//      productInfoRepository.insert(products)
//    }

    def ingestProductReviews: Future[InsertManyResult] = {

      logger.info("[DatasetIngestionService][ingestProductReviews] About to start.")

      val reviews: List[ProductReview] = CSVReader[ProductReview].readCSVFromFileName(appConfig.reviewsFilepath)

      logger.info("[DatasetIngestionService][ingestProductReviews] Inserting data from dataset")
      logger.info(s"[DatasetIngestionService][ingestProductReviews] Dataset size - ${reviews.size}")

      productReviewsRepository.insert(reviews)
    }

//    ingestProductInfo.flatMap { _ =>
      ingestProductReviews.map {
        result =>
          logger.info(s"[DatasetIngestionService] Done. ${result.wasAcknowledged()}")
          result
      }
//    }
  }
}
