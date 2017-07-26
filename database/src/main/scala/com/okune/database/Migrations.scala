package com.okune.database

import com.okune.database.CorePgDriver.api._
import scala.language.reflectiveCalls
import scala.concurrent.Await
import scala.concurrent.duration._
import com.typesafe.config.Config
import com.okune.database.CorePgDriver.DDL

object Migrations {

  /** Postgres database migration class
    *
    * This comes in handy when you need services to create their own database and tables on startup in order to reduce manual setup
    *
    * @param config   typesafe `Config` object for postgres
    * @param schema   a Seq of slick `DDL`. Example `val schema = Some(myTableQuery1.schema ++ myTableQuery2.schema)`
    * @param sqlStmts a sequence of raw sql statements
    */
  class Postgres(config: Config, schema: Option[DDL] = None, sqlStmts: Seq[String] = Nil) {
    val driver = "org.postgresql.Driver"
    val host = config.getString("properties.serverName")
    val port = config.getInt("properties.portNumber")
    val user = config.getString("properties.user")
    val password = config.getString("properties.password")
    val dbName = config.getString("properties.databaseName")

    val defaultDbUri = s"jdbc:postgresql://$host:$port/postgres"
    val dbUri = s"jdbc:postgresql://$host:$port/$dbName"

    /** Creates database and tables  */
    def createAll(): Unit = {
      createDatabase()
      createTables()
    }

    /** Creates a database if it does not exist */
    def createDatabase(): Unit = {
      using(db(defaultDbUri)) { db =>
        val checkDbResult = Await.result(db.run(sql"SELECT 1 FROM pg_database WHERE datname = $dbName".as[Int]), Duration.Inf)
        if (checkDbResult.isEmpty) {
          Await.result(db.run(sqlu"CREATE DATABASE #$dbName"), Duration.Inf)
        }
      }
    }

    /** Force drops a database if it exists */
    def dropDatabase(): Unit = {
      using(db(defaultDbUri)) { db =>
        val checkDbResult = Await.result(db.run(sql"SELECT 1 FROM pg_database WHERE datname = $dbName".as[Int]), Duration.Inf)
        if (checkDbResult.nonEmpty) {
          Await.result(db.run(sql"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = $dbName".as[String]), Duration.Inf)
          Await.result(db.run(sqlu"DROP DATABASE #$dbName"), Duration.Inf)
        }
      }
    }

    /** Creates tables given slick table schema */
    def createTables(): Unit = {
      using(db(dbUri)) { db =>
        schema.foreach(s => db.run(DBIO.seq(s.create)))
      }
    }

    /** Runs sql migrations */
    def sqlMigration(): Unit = {
      using(db(dbUri)) { db =>
        sqlStmts.foreach(stmt => db.run(sqlu"$stmt"))
      }
    }

    /** A convenient function for safely closing a resource after use */
    private def using[A <: {def close() : Unit}, B](resource: A)(f: A => B): B =
      try {
        f(resource)
      } finally {
        if (resource != null) resource.close()
      }

    /** Creates `Database` object for running `DBIOAction` */
    private def db(uri: String): Database = Database.forURL(uri, user = user, password = password, driver = driver)
  }
}