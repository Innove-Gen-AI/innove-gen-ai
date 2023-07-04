/*
 * Copyright 2023 Innové Gen AI
 *
 */

package controllers

import actions.RequestBodyAction.RequestBodyActionBuilder
import models.search.ProductImageRequest
import play.api.mvc._
import repositories.ProductImageRepository
import services.search.CustomSearchService

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class ProductImageSearchController @Inject()(customSearchService: CustomSearchService,
                                             productImageRepository: ProductImageRepository)
                                            (implicit val controllerComponents: ControllerComponents, ec: ExecutionContext) extends BaseController {

  def getProductImagesCsv: Action[AnyContent] = Action.async { implicit request =>
    productImageRepository.getProductImages.map { results =>
      val outputStart = "product_id,image\n"
      val output = results.map(result => s"${result.product_id},${result.image}").mkString("\n")
      if (results.nonEmpty) Ok(outputStart + output) else NoContent
    }
  }

  def getProductImages(batch: Int = 1): Action[ProductImageRequest] = Action.validateBodyAs[ProductImageRequest].async { implicit request =>
    customSearchService.handle(cx = request.body.cx, api = request.body.api, batch).map { results =>
      val outputStart = "product_id,image\n"
      val output = results.map(result => s"${result.product_id},${result.image}").mkString("\n")
      if(results.nonEmpty) Ok(outputStart + output) else NoContent
    }
  }
}
