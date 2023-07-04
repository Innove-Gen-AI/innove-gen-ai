/*
 * Copyright 2023 Innové Gen AI
 *
 */

package services.search

import connector.CustomSearchConnector
import models.search.{CustomSearchResponse, ProductImage}
import play.api.Logging
import repositories.{ProductImageRepository, ProductInfoRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomSearchService @Inject()(customSearchConnector: CustomSearchConnector,
                                    productInfoRepository: ProductInfoRepository,
                                    productImageRepository: ProductImageRepository)
                                   (implicit ec: ExecutionContext) extends Logging {

  def handle(cx: String, api: String, batch: Int): Future[Seq[ProductImage]] = {
    fetchImagesForProducts(cx, api, batch).flatMap { productImages =>
      if(productImages.nonEmpty){
        val imageUpdates = Future.sequence(
          productImages.map{ productImage =>
            productInfoRepository.updateWithImage(productImage)
          }
        ).map(_.flatten)

        imageUpdates.flatMap { _ =>
          productImageRepository.insert(productImages).map(_ => productImages)
        }
      } else {
        logger.info("[CustomSearchService][handle] No product images found.")
        Future.successful(Seq.empty)
      }
    }
  }

  private def fetchImagesForProducts(cx: String, api: String, batch: Int): Future[Seq[ProductImage]] = {
    productInfoRepository.getProductsWithoutImage.flatMap { products =>
      if(products.nonEmpty){
        logger.info(s"[CustomSearchService][fetchImagesForProducts] Products left to fetch - ${products.size}.")
        logger.info(s"[CustomSearchService][fetchImagesForProducts] About to fetch ${batch} images.")
        Future.sequence(products.take(batch).map { product =>
          customSearchConnector.callCustomSearchAPI(
            product_name = product.product_name,
            brand_name = product.brand_name,
            cx = cx,
            api = api
          ).map {
            case Right(CustomSearchResponse(items)) =>
              if(items.nonEmpty){
                Some(ProductImage(product.product_id, items.head.link))
              } else {
                None
              }
            case Left(_) => None
          }
        }).map(_.flatten)
      } else {
        logger.info("[CustomSearchService][fetchImagesForProducts] No products without images found.")
        Future.successful(Seq.empty)
      }
    }
  }
}
