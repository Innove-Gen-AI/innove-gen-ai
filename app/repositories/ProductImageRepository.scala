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

import models.search.ProductImage
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.result.InsertManyResult
import play.api.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProductImageRepository @Inject ()(implicit ec: ExecutionContext) extends Repository with Logging {

  val name = "productImage"
  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[ProductImage]), DEFAULT_CODEC_REGISTRY)
  val collection: MongoCollection[ProductImage] = database.withCodecRegistry(codecRegistry).getCollection(name)

  val indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("product_id"), IndexOptions().unique(true))
  )

  def getProductImages: Future[Seq[ProductImage]] = {
    collection.find[ProductImage]().toFuture()
  }

  def insert(productImages: Seq[ProductImage]): Future[InsertManyResult] = {
    collection.createIndexes(indexes).toFuture().flatMap {
      result =>
        logger.info(s"[ProductImageRepository] createIndexes result = $result")
        logger.info(s"[ProductImageRepository] Inserting ${productImages.size} product images..")
        collection.insertMany(productImages).toFuture().map {
          result =>
            logger.info(s"[ProductImageRepository] Done inserting ${productImages.size} product images.")
            result
        }
    }
  }

  def count: Future[Long] = collection.countDocuments().toFuture()
}
