organization in ThisBuild := "com.okune"

val commonDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

lazy val root =
  (project in file("."))
    .aggregate(
      database)
    .settings(
      publishArtifact := false)

lazy val database =
  Project(id = "core-database", base = file("database"))
    .settings(libraryDependencies ++= commonDependencies)
    .settings(
      libraryDependencies ++=
        Seq("org.reactivemongo" 	        %% "reactivemongo" 	      % "0.12.5",
	          "org.postgresql"       	       % "postgresql"           % "42.1.1",
            "com.typesafe.slick"  	      %% "slick"                % "3.2.1",
            "com.typesafe.slick"  	      %% "slick-hikaricp"       % "3.2.1",
            "com.github.tminglei" 	      %% "slick-pg"             % "0.15.2",
            "com.github.tminglei" 	      %% "slick-pg_spray-json"  % "0.15.2"),
      publishArtifact in(Compile, packageSrc) := false)

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
