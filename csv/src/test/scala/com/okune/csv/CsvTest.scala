package com.okune.csv

import org.scalatest._

class CsvTest extends PropSpec with Matchers {
  import java.io._
  import org.scalacheck._
  import prop.PropertyChecks._

  /** Check that the data we read is the data we wrote. */
  property("read/write") {
    forAll(genData, genLineSeparator, genDelimiter) { (data, lineSeparator, delimiter) =>
      val baos = new ByteArrayOutputStream
      val writer = CSVWriter(baos, delimiter, lineSeparator)
      try data foreach (line => writer(line.toArray))
      finally writer.close

      val readData = CSVReader(new ByteArrayInputStream(baos.toByteArray), Some(delimiter), Some(lineSeparator)).map(_.toList).toList

      readData should be (data)
    }
  }

  lazy val genLineSeparator: Gen[String] = Gen.oneOf("\n", "\r", "\r\n")

  lazy val genDelimiter: Gen[Char] = Gen.oneOf(',', '|', '\t')

  lazy val genData: Gen[List[List[String]]] =
    for {
      numRows <- Gen.choose(1, 100)
      numCols <- Gen.choose(1, 1024)
      columns  = Gen.containerOfN[List, String](numCols, Gen.identifier)
      rows    <- Gen.containerOfN[List, List[String]](numRows, columns)
    } yield rows
}
