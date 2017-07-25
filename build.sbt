organization in ThisBuild := "com.okune"

scalaVersion in ThisBuild := "2.12.2"

scalacOptions in ThisBuild :=
  Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:higherKinds",
    "-unchecked",
    "-Xfuture",
    "-Xlint",
    "-Xmax-classfile-name", "254",
    "-Ywarn-unused-import")

javacOptions in ThisBuild ++=
  Seq(
    "-source", "1.8",
    "-target", "1.8")

val slickV = "3.2.1"
val slickPgV = "0.15.2"
val hadoopV = "2.8.1"

val commonDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.typesafe" % "config" % "1.3.1",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

lazy val root =
  (project in file("."))
    .aggregate(
      database,
      hdfs)
    .settings(
      publishArtifact := false)

lazy val database =
  Project(id = "core-database", base = file("database"))
    .settings(libraryDependencies ++= commonDependencies)
    .settings(
      libraryDependencies ++=
        Seq("org.reactivemongo" 	        %% "reactivemongo" 	      % "0.12.5",
	          "org.postgresql"       	       % "postgresql"           % "42.1.1",
            "com.typesafe.slick"  	      %% "slick"                % slickV,
            "com.typesafe.slick"  	      %% "slick-hikaricp"       % slickV,
            "com.github.tminglei" 	      %% "slick-pg"             % slickPgV,
            "com.github.tminglei" 	      %% "slick-pg_spray-json"  % slickPgV),
      publishArtifact in(Compile, packageSrc) := false)


lazy val hdfs =
  Project(id = "core-hdfs", base = file("hdfs"))
    .settings(libraryDependencies ++= commonDependencies)
    .settings(
      libraryDependencies ++= Seq(
        "org.apache.hadoop" % "hadoop-aws" % hadoopV,
        "com.google.cloud.bigdataoss" % "gcs-connector" % "1.6.1-hadoop2",
        "org.apache.hadoop" % "hadoop-client" % hadoopV,
        "commons-codec" % "commons-codec" % "1.10"),
      fork in Test := true,
      publishArtifact in(Compile, packageSrc) := false)