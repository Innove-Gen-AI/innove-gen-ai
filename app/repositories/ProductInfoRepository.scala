/*
 * Copyright 2020 HM Revenue & Customs
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
 */

package repositories

import com.mongodb.client.model.ReturnDocument
import models.dataset.{PrimaryProductInfo, ProductInfo}
import models.search.ProductImage
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.result.InsertManyResult
import play.api.Logging
import play.api.libs.json.Json

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProductInfoRepository @Inject ()(implicit ec: ExecutionContext) extends Repository with Logging {

  val name = "productInfo"
  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[ProductInfo]), DEFAULT_CODEC_REGISTRY)
  val collection: MongoCollection[ProductInfo] = database.withCodecRegistry(codecRegistry).getCollection(name)

  val indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("product_id"), IndexOptions().unique(true)),
    IndexModel(Indexes.ascending("product_name"), IndexOptions()),
    IndexModel(Indexes.ascending("brand_id"), IndexOptions()),
    IndexModel(Indexes.ascending("brand_name"), IndexOptions()),
    IndexModel(Indexes.ascending("image"), IndexOptions().sparse(true))
  )

  private def lookupQuery(productId: String): Bson = equal("product_id", productId)

  def getProduct(productId: String): Future[Option[ProductInfo]] = {
    collection.find[ProductInfo](lookupQuery(productId)).headOption()
  }

  def getProducts: Future[Seq[PrimaryProductInfo]] = {
    collection.find[Document]().projection(include("product_id", "product_name", "brand_name")).toFuture().map { documents =>
      documents.map { document =>
        Json.parse(document.toJson()).as[PrimaryProductInfo]
      }
    }
  }

  def getProductsWithoutImage: Future[Seq[PrimaryProductInfo]] = {

    def missingLookupQuery: Bson = equal("image", null)

    collection.find[Document](missingLookupQuery).projection(include("product_id", "product_name", "brand_name")).toFuture().map { documents =>
      documents.map { document =>
        Json.parse(document.toJson()).as[PrimaryProductInfo]
      }
    }
  }

  def updateWithImage(productImage: ProductImage): Future[Option[ProductInfo]] = {

    val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    def set: BsonDocument = BsonDocument("$set" -> BsonDocument("image" -> productImage.image))

    collection.findOneAndUpdate(lookupQuery(productImage.product_id), set, options).toFutureOption()
  }

  def insert(products: Seq[ProductInfo]): Future[InsertManyResult] = {
    collection.createIndexes(indexes).toFuture().flatMap {
      result =>
        logger.info(s"[ProductInfoRepository] createIndexes result = $result")
        logger.info(s"[ProductInfoRepository] Inserting ${products.size} products..")
        collection.insertMany(products).toFuture().map {
          result =>
            logger.info(s"[ProductInfoRepository] Done inserting ${products.size} products.")
            result
        }
    }
  }

  def count: Future[Long] = collection.countDocuments().toFuture()
}
