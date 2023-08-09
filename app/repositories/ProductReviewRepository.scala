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

package repositories

import models.dataset.ProductReview
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.result.InsertManyResult
import play.api.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProductReviewRepository @Inject ()(implicit ec: ExecutionContext) extends Repository with Logging {

  val name = "productReview"
  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[ProductReview]), DEFAULT_CODEC_REGISTRY)
  val collection: MongoCollection[ProductReview] = database.withCodecRegistry(codecRegistry).getCollection(name)

  val indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("product_id"), IndexOptions()),
    IndexModel(Indexes.ascending("submission_time"), IndexOptions()),
    IndexModel(Indexes.ascending("rating"), IndexOptions())
  )

  private def lookupQuery(productId: String): Bson = equal("product_id", productId)

  private def negativeQuery: Bson = BsonDocument("rating" -> BsonDocument("$lte" -> 3))
  private def positiveQuery: Bson = BsonDocument("rating" -> BsonDocument("$gte" -> 3))
  private def recentQuery: Bson = BsonDocument("submission_time" -> -1)

  def getProductReviews(productId: String, positiveFilter: Option[Boolean], recentFilter: Boolean): Future[Seq[ProductReview]] = {

    val positiveOptionQuery: Option[Bson] = positiveFilter match {
      case Some(true) => Some(positiveQuery)
      case Some(false) => Some(negativeQuery)
      case None => None
    }

    val recentOptionQuery: Bson = if (recentFilter) recentQuery else BsonDocument()

    val queryContents: Seq[Bson] = Seq(
      Some(lookupQuery(productId)),
      positiveOptionQuery
    ).flatten

    val query: Bson = Filters.and(queryContents: _*)

    collection.find[ProductReview](query).sort(recentOptionQuery).toFuture()
  }

  def countProductReviews(productId: String): Future[Long] = collection.countDocuments(lookupQuery(productId)).toFuture()

  def insert(reviews: Seq[ProductReview]): Future[InsertManyResult] = {
    collection.createIndexes(indexes).toFuture().flatMap{
      result =>
        logger.info(s"[ProductReviewRepository] createIndexes result = $result")

        logger.info(s"[ProductReviewRepository] Inserting ${reviews.size} reviews..")
        collection.insertMany(reviews).toFuture().map {
          result =>
            logger.info(s"[ProductReviewRepository] Done inserting ${reviews.size} reviews.")
            result
        }
    }
  }

  def count: Future[Long] = collection.countDocuments().toFuture()
}
