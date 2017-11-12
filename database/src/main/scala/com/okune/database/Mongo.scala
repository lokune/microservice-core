package com.okune.database

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.typesafe.config.Config
import reactivemongo.api.{Cursor, DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.document

trait MongoDb {

  /** mongo configuration */
  def config: Config

  val host: String = config.getString("host")
  val port: Int = config.getInt("port")
  val database: String = config.getString("database")
  val authMode: String = config.getString("authMode")
  val mongoUri: String = s"mongodb://$host:$port/$database?authMode=$authMode"
  val driver: MongoDriver = MongoDriver()

  implicit val ec: ExecutionContext

  lazy val db: Future[DefaultDB] = for {
    uri <- Future.fromTry(MongoConnection.parseURI(mongoUri))
    con = driver.connection(uri)
    dn <- Future(uri.db.get)
    db <- con.database(dn)
  } yield db
}

/** A generic `Data Access Object` for `Mongo DB`
  *
  * Get Basic `CRUD` on mongo collections for free
  */
trait MongoDao[A] {

  type Count = Int

  type ID = BSONObjectID

  /** `BSONCollection` object. Create it from the db which is `Future[DefaultDB]` */
  def collection(): Future[BSONCollection]

  /** insert an item of any type `A` */
  def insert(item: A)(implicit ec: ExecutionContext, writer: BSONDocumentWriter[A]): Future[Count] =
    collection().flatMap(_.insert(item).map(_.n))


  /** find an item of any type `A` by `id` */
  def findById(id: ID)(implicit ec: ExecutionContext, reader: BSONDocumentReader[A]): Future[Option[A]] =
    for {
      result <- collection().flatMap(_.find(document("_id" -> id)).
        cursor[A]().collect[List](-1, Cursor.FailOnError[List[A]]()))
    } yield result.headOption

  /** find all items of any type `A` in collection */
  def findAll()(implicit ec: ExecutionContext, reader: BSONDocumentReader[A]): Future[List[A]] =
    collection().flatMap(_.find(document()).
      cursor[A]().collect[List](-1, Cursor.FailOnError[List[A]]()))

  /** delete an item of any type `A` */
  def delete(id: ID)(implicit ec: ExecutionContext): Future[Count] = {
    val query = BSONDocument(
      "_id" -> id)
    collection().flatMap(_.remove(query).map(_.n))
  }
}