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

package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.ProductService

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class ProductsController @Inject()(productService: ProductService)
                                  (implicit val controllerComponents: ControllerComponents, ec: ExecutionContext) extends BaseController {

  def index: Action[AnyContent] = Action { implicit request =>
    NoContent
  }

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
