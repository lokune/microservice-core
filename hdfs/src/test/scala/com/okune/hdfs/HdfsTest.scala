package com.okune.hdfs

import org.apache.hadoop.io.IOUtils
import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success, Try}

class HdfsTest extends WordSpec with Matchers {

  import java.io._
  import java.net.URI
  import org.apache.hadoop.conf.Configuration


  /**
    * We are going to write/read to/from s3 bucket. With
    */
  "The script " should {
    "be able to write/read to/fro S3" in {
      val tmp = {
        val t = java.nio.file.Files.createTempFile("HdfsTest", null).toFile
        Some(new PrintWriter(t)).foreach { p => p.write("hello world\n"); p.close }
        t.deleteOnExit
        t
      }

      val bucket: String = "dev-tests-lokune"
      val hdfsUri: URI = new URI(s"s3a://${bucket}/" + tmp.getName)
      write(tmp.toURI, hdfsUri, Hdfs.config()) match {
        case Success(_) =>
          println("File written to destination.")
        case Failure(e) =>
          throw new Exception("Unable to write file to destination.", e)
      }

      val numberOfBytes: Long = Hdfs.getFileSize(hdfsUri)
      numberOfBytes should be(12)
      val content = read(hdfsUri)
      content.head should be("hello world")
      // cleanup
      delete(hdfsUri)
    }
  }

  def read(uri: URI) = {
    val is = Hdfs.read(uri)
    val lines = scala.io.Source.fromInputStream(is).getLines
    lines.toList
  }

  def write(from: URI, to: URI, config: Configuration): Try[Unit] = {
    val is = Hdfs.read(from)
    val os = Hdfs.write(to)
    Try(IOUtils.copyBytes(is, os, config, true))
  }

  def delete(uri: URI) =
    Hdfs.fileSystem(uri) delete(Hdfs.path(uri), true)
}
