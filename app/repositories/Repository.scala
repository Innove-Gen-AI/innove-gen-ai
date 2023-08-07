/*
 * Copyright 2023 Innové Gen AI
 *
 */

package repositories

import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala._

import javax.inject.Singleton

@Singleton
class Mongo {
  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
  val database: MongoDatabase = mongoClient.getDatabase("innove-gen-ai")
}

trait Repository extends Mongo {

  val name: String
  val codecRegistry: CodecRegistry
  val collection: MongoCollection[_]
}
