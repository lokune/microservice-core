package com.okune.database

import com.github.tminglei.slickpg._
import slick.jdbc.JdbcBackend.Database
import slick.basic.Capability

import scala.concurrent.Future

/** Slick extensions for PostgreSQL, to support a series of pg data types and related operators/functions. */
trait CorePgDriver extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport
  with PgSprayJsonSupport {

  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `JdbcCapabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = CorePgAPI

  object CorePgAPI extends API
    with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

}

object CorePgDriver extends CorePgDriver

trait PostgresDb {
  def configPath: String

  implicit lazy val db = Database.forConfig(configPath)
}

/** A generic `Data Access Object` for `Postgres DB`
  *
  * Get some `CRUD` on postgres tables for free
  */

import CorePgDriver.api._
import scala.concurrent.ExecutionContext

trait IdentifiableEntity[ID] {
  def id: Option[ID]
}

trait IdentifiableTable[ID] {
  def id: Rep[ID]
}

abstract class PostgresDao[A <: IdentifiableEntity[ID], T <: Table[A] with IdentifiableTable[ID], ID: BaseColumnType] {

  type Count = Int

  /** `TableQuery` of any type `T` which is a subtype of  `Table` of any type `A` */
  def tableQuery: TableQuery[T]

  /** insert an item of any type `A` */
  def insert(item: A)(implicit db: Database): Future[ID] =
    db.run((tableQuery returning tableQuery.map(_.id)) += item)

  /** insert items of any type `A` */
  def insertBulk(items: List[A])(implicit db: Database): Future[Option[Count]] =
    db.run(tableQuery ++= items)

  /** update an item of any type `A` */
  def update(item: A)(implicit db: Database): Future[Count] =
    db.run(tableQuery.filter(_.id === item.id).update(item))

  /** update an item of any type `A`, return updated item */
  def updateGet(item: A)(implicit db: Database, ec: ExecutionContext): Future[A] = for {
    count <- update(item)
    if count > 0
  } yield item

  /** find an item of any type `A` by `id` */
  def findById(id: ID)(implicit db: Database, ec: ExecutionContext): Future[Option[A]] =
    db.run(tableQuery.filter(_.id === id).result map (_.headOption))

  /** find all items of any type `A` in table */
  def findAll()(implicit db: Database): Future[Seq[A]] = db.run(tableQuery.result)

  /** delete item of any type `A` by it's id */
  def delete(id: ID)(implicit db: Database): Future[Count] =
    db.run(tableQuery.filter(_.id === id).delete)
}