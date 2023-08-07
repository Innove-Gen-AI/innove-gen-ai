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
import models.dataset.{ProductImage, ProductInfo, ProductReview}
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
          logger.info("[DatasetIngestionService][ingestProductInfo] Already has data. Continuing.")
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
          logger.info("[DatasetIngestionService][ingestProductReviews] Already has data. Continuing.")
          Future.successful(true)
        }
      }
    }

    def ingestProductImages: Future[Boolean] = {
      productInfoRepository.getProductsWithoutImage.flatMap { productsWithoutImage =>
        if(productsWithoutImage.nonEmpty){

          logger.info("[DatasetIngestionService][ingestProductImages] About to start.")

          val images: List[ProductImage] = CSVReader[ProductImage].readCSVFromFileName(appConfig.imagesFilepath, delimiter = '|').filter(_.toOption.isDefined).map(_.get)

          logger.info("[DatasetIngestionService][ingestProductImages] Inserting data from dataset")

          val toUpdate = images.filter(image => productsWithoutImage.map(_.product_id).contains(image.product_id))
          logger.info(s"[DatasetIngestionService][ingestProductImages] Dataset size - ${toUpdate.size}")

          Future.sequence(toUpdate.map { productImage =>
            productInfoRepository.updateWithImage(productImage = productImage)
          }).map(_.flatten).map { result =>
            result.size == productsWithoutImage.size
          }
        } else {
          logger.info("[DatasetIngestionService][ingestProductImages] Already has data. Continuing.")
          Future.successful(true)
        }
      }
    }

    ingestProductInfo.flatMap { _ =>
      ingestProductReviews.flatMap { _ =>
        ingestProductImages.map { result =>
          logger.info(s"[DatasetIngestionService] Finished dataset ingestion.")
          result
        }
      }
    }
  }
}
