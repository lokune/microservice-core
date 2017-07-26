package com.okune.hdfs

import java.io.{BufferedOutputStream, File, FileOutputStream}

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration

object Hdfs {

  import java.net.URI
  import org.apache.hadoop.fs._

  /** Create a Hadoop `Configuration` value for use in executing `Hdfs` operations.
    *
    * Default configuration loading rules are explained at https://hadoop.apache.org/docs/r2.7.1/api/org/apache/hadoop/conf/Configuration.html.
    *
    * If access to `gs://` URIs are needed, the following configuration keys MUST be set:
    *
    * $ - `com.okune.hdfs.google.cloud.auth.service.account.email`
    * $ - `com.okune.hdfs.google.cloud.auth.service.account.keyfile`
    * $ - `com.okune.hdfs.fs.gs.project.id`
    *
    * If access to `s3a://` URIs are needed, the following configuration keys MUST be set:
    *
    * $ - `com.okune.hdfs.fs.s3a.access.key`
    * $ - `com.okune.hdfs.fs.s3a.secret.key`
    *
    * 1. AWS cli is useful; especially to check if an encryption is applied. (Ref. [1, 2])
    *
    * $ aws s3api get-object --bucket <bucketname> --key <remote-file> <local-file>
    *
    * If an encryption is applied, the meta data states
    * "ServerSideEncryption": "AES256"
    *
    * References
    *
    * [1] AWS Command Line Interface
    * https://aws.amazon.com/cli/
    * [2] Add s3 server-side encryption
    * https://issues.apache.org/jira/browse/HADOOP-10568
    *
    * @return the hadoop `Configuration`
    */
  def configuration(root: Config = ConfigFactory.load(), ioBufferSize: Int = 4096): Configuration = {
    val config = root.getConfig("com.okune.hdfs")
    val conf = new Configuration()

    // GCS support
    conf.setBoolean("google.cloud.auth.service.account.enable", true)
    if (config.hasPath("google.cloud.auth.service.account.email")) conf.set("google.cloud.auth.service.account.email", config.getString("google.cloud.auth.service.account.email"))
    if (config.hasPath("google.cloud.auth.service.account.base64.keyfile"))
      conf.set("google.cloud.auth.service.account.keyfile", toFile(config.getString("google.cloud.auth.service.account.base64.keyfile")).getCanonicalPath)
    if (config.hasPath("fs.gs.project.id")) {
      conf.set("fs.gs.project.id", config.getString("fs.gs.project.id"))
      conf.set("fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      conf.set("fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
    } // end gcs support

    // S3 support
    val (awsAccessKey, awsSecretAccessKey, awsServerSideEncryptionAlgo): (String, String, String) = ("fs.s3a.access.key", "fs.s3a.secret.key", "fs.s3a.server-side-encryption-algorithm")
    if (config.hasPath(awsAccessKey)) {
      conf.set(awsAccessKey, config.getString(awsAccessKey))
    }
    if (config.hasPath(awsSecretAccessKey)) {
      conf.set(awsSecretAccessKey, config.getString(awsSecretAccessKey))
    }
    if (config.hasPath(awsServerSideEncryptionAlgo)) {
      conf.set(awsServerSideEncryptionAlgo, config.getString(awsServerSideEncryptionAlgo))
    } // end s3 support

    conf.setInt("io.file.buffer.size", ioBufferSize)

    conf
  }

  /** Create file from base64 String **/
  private def toFile(base64String: String): File = {
    //Use apache commons codec lib(Reliable and supports older java versions)
    val fileContents: Array[Byte] = new Base64().decode(base64String)
    val tmpKeyFile = {
      val t = java.nio.file.Files.createTempFile("GC_KeyFile", null).toFile
      val bos = new BufferedOutputStream(new FileOutputStream(t))
      bos.write(fileContents)
      bos.close
      t.deleteOnExit
      t
    }

    tmpKeyFile
  }

  /** Return the current `Configuration`. */
  def config(): Configuration = configuration()

  /** Convert a `URI` to a Hadoop `Path`. */
  def path(uri: URI): Path = new Path(uri)

  /** Get the Hadoop `FileSystem` for a path. */
  def fileSystem(path: Path): FileSystem =
    path.getFileSystem(config())

  /** Resolve a child path against a parent hadoop `Path`. */
  def path(parentPath: Path, child: String): Path = new Path(parentPath, child)

  /** Check if file exists. */
  def exists(fs: FileSystem, path: Path): Boolean = fs.exists(path)

  /** Get the Hadoop `FileSystem` for a `URI`. */
  def fileSystem(uri: URI): FileSystem =
    fileSystem(path(uri))

  /** Read bytes from a HDFS-supported `URI`. */
  def read(uri: URI): java.io.InputStream = {
    val fs = fileSystem(uri)
    fs.open(path(uri))
  }

  /** Write bytes to a HDFS-supported `URI`. */
  def write(uri: URI): java.io.OutputStream = {
    val fs = fileSystem(uri)
    fs.create(path(uri))
  }

  /** Get size of file at `URI`. */
  def getFileSize(uri: URI): Long = {
    val fs = fileSystem(uri)
    val status = fs.getFileStatus(path(uri))
    if (status.isDirectory) {
      fs.listStatus(path(uri)).map(_.getLen()).sum
    } else status.getLen
  }
}
