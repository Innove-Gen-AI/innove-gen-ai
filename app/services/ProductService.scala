/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services

import models.dataset.{PrimaryProductInfo, ProductInfo}
import play.api.Logging
import repositories.{ProductInfoRepository, ProductReviewRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProductService @Inject()(productInfoRepository: ProductInfoRepository,
                               productReviewsRepository: ProductReviewRepository)
                              (implicit ec: ExecutionContext) extends Logging {

  def getProducts: Future[Seq[PrimaryProductInfo]] = {
    productInfoRepository.getProducts.flatMap { products =>
      Future.sequence(products.map { product =>
          productReviewsRepository.countProductReviews(product.product_id).map { count =>
            if(count > 0) Some(product) else None
          }
      }).map(_.flatten)
    }
  }

  def getProduct(productId: String): Future[Option[ProductInfo]] ={
    productInfoRepository.getProduct(productId).flatMap {
      case Some(product) =>
        productReviewsRepository.countProductReviews(product.product_id).map { count =>
          if (count > 0) Some(product) else None
        }
      case None => Future.successful(None)
    }
  }

  def getProductReviews(productId: String, filters: Seq[String], method: String): Future[Seq[String]] = {

    val positive = filters.contains("positive")
    val negative = filters.contains("negative")

    val positiveFilter: Option[Boolean] = {
      (positive, negative) match {
        case (true, _) => Some(true)
        case (_, true) => Some(false)
        case _ => None
      }
    }

    val recentFilter: Boolean = filters.contains("recent")

    productReviewsRepository.getProductReviews(productId, positiveFilter, recentFilter).map { _reviews =>
      val reviews = _reviews.map(_.review_text)

      logger.info(s"[ProductService][getProductReviews][$method] Reviews found for product id: $productId, Total reviews: ${reviews.length}")
      if (recentFilter) reviews else scala.util.Random.shuffle(reviews)
    }
  }
}
