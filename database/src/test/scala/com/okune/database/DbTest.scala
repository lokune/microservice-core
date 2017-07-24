package com.okune.database

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag
import com.okune.database.CorePgDriver.api._
import com.typesafe.config.ConfigFactory
import reactivemongo.api.collections.bson.BSONCollection

class PgTest extends FunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    //Create database and tables
    Postgres.Migrations.init()
  }

  override def afterAll(): Unit = {
    //Drop database
    Postgres.Migrations.destroy()
  }

  test("save and/or retrieve data from postgres db") {
    import Postgres._

    var testUserId = 0L
    try {
      val testUser = User(None, "laban.okune@gmail.com", "123")
      testUserId = Await.result(Postgres.UserDao.insert(testUser), Duration.Inf)
      assert(testUserId > 0)

      val existingUsers = Await.result(Postgres.UserDao.findAll(), Duration.Inf)
      assert(existingUsers.size > 0)

      val savedUser = Await.result(Postgres.UserDao.findById(testUserId), Duration.Inf)
      assert(savedUser.nonEmpty)
    } finally {
      //cleanup
      if (testUserId != 0L) Await.result(Postgres.UserDao.delete(testUserId), Duration.Inf)
    }
  }
}

class MongoTest extends FunSuite {
  test("save and/or retrieve data from mongo") {
    import Mongo._

    val person = Person(BSONObjectID.generate(), "Okune", 16)
    Await.result(Mongo.PersonDao.insert(person), Duration.Inf)
    val storedList = Await.result(Mongo.PersonDao.findById(person._id), Duration.Inf)
    assert(storedList.size > 0)
    val query = BSONDocument("name" -> storedList.head.name)

    //cleanup
    Await.result(Mongo.PersonDao.collection.flatMap(_.remove(query)), Duration.Inf)
  }
}

object Mongo extends MongoDb {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

  def config = ConfigFactory.load().getConfig("com.okune.database.mongo")

  case class Person(_id: BSONObjectID, name: String, Age: Int)

  object Person {
    implicit def writer: BSONDocumentWriter[Person] = Macros.writer[Person]

    implicit def reader: BSONDocumentReader[Person] = Macros.reader[Person]
  }

  object PersonDao extends MongoDao[Person] {
    val collectionName = "tests"
    override val collection: Future[BSONCollection] = db.map { ddb =>
      val collection: BSONCollection = ddb.collection(collectionName)
      collection.indexesManager.ensure(Index(key = Seq(("name", IndexType.Ascending)), unique = true))
      collection
    }
  }

}

object Postgres extends PostgresDb {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

  def configPath = "com.okune.database.postgres"

  case class User(id: Option[Long], email: String, password: String) extends IdentifiableEntity[Long]

  class UserTable(tag: Tag) extends Table[User](tag, "users") with IdentifiableTable[Long] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def email = column[String]("email")

    def password = column[String]("password")

    def userEmailUnq = index("emailUnq", email, unique = true)

    def * = (id.?, email, password) <> (User.tupled, User.unapply)
  }

  object UserDao extends PostgresDao[User, UserTable, Long] {
    def tableQuery = TableQuery[UserTable]
  }


  /*
   * This shows how we can use microservice-core migrations api
   */
  object Migrations {
    val config = ConfigFactory.load().getConfig(s"${configPath}")
    val schema = TableQuery[UserTable].schema
    val migrationsRunner = new com.okune.database.Migrations.Postgres(config, Some(schema))

    def init(): Unit = {
      migrationsRunner.createAll()
    }

    def destroy(): Unit = {
      migrationsRunner.dropDatabase()
    }
  }
}
