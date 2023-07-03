/*
 * Copyright 2023 Innové Gen AI
 *
 */

package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.ProductService

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class ProductsController @Inject()(productService: ProductService)
                                  (implicit val controllerComponents: ControllerComponents, ec: ExecutionContext) extends BaseController {

  def getProducts: Action[AnyContent] = Action.async { implicit request =>
    productService.getProducts.map { products =>
      if(products.nonEmpty) Ok(Json.toJson(products)) else NoContent
    }
  }

  def getProduct(productId: String): Action[AnyContent] = Action.async { implicit request =>
    productService.getProduct(productId).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound
    }
  }
}
