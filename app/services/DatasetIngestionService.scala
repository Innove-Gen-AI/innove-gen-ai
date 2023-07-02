/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services

import config.AppConfig
import models.dataset.{ProductInfo, ProductReview}
import play.api.Logging
import repositories.{ProductInfoRepository, ProductReviewRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DatasetIngestionService @Inject()(productInfoRepository: ProductInfoRepository,
                                        productReviewsRepository: ProductReviewRepository,
                                        appConfig: AppConfig)
                                       (implicit ec: ExecutionContext) extends Logging {

  def ingestDatasetFiles(): Future[Boolean] = {

    import purecsv.safe._

    def ingestProductInfo: Future[Boolean] = {
      productInfoRepository.count.flatMap { count =>
        if(count == 0){
          logger.info("[DatasetIngestionService][ingestProductInfo] About to start.")

          val products: List[ProductInfo] = CSVReader[ProductInfo].readCSVFromFileName(appConfig.productsFilepath).filter(_.toOption.isDefined).map(_.get)

          logger.info("[DatasetIngestionService][ingestProductInfo] Inserting data from dataset")
          logger.info(s"[DatasetIngestionService][ingestProductInfo] Dataset size - ${products.size}")

          productInfoRepository.insert(products).map(_.wasAcknowledged())
        } else {
          Future.successful(true)
        }
      }
    }

    def ingestProductReviews: Future[Boolean] = {
      productReviewsRepository.count.flatMap { count =>
        if (count == 0) {
          logger.info("[DatasetIngestionService][ingestProductReviews] About to start.")

          val reviews: List[ProductReview] = CSVReader[ProductReview].readCSVFromFileName(appConfig.reviewsFilepath).filter(_.toOption.isDefined).map(_.get)

          logger.info("[DatasetIngestionService][ingestProductReviews] Inserting data from dataset")
          logger.info(s"[DatasetIngestionService][ingestProductReviews] Dataset size - ${reviews.size}")

          productReviewsRepository.insert(reviews).map(_.wasAcknowledged())
        } else {
          Future.successful(true)
        }
      }
    }

    ingestProductInfo.flatMap { _ =>
      ingestProductReviews.map { result =>
        logger.info(s"[DatasetIngestionService] Done.")
        result
      }
    }
  }
}
