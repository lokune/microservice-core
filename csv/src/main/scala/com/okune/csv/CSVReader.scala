package com.okune.csv

object CSVReader {

  import com.univocity.parsers.csv._
  import org.apache.commons.io.input.BOMInputStream

  /** Read a CSV (or TSV, etc) as an `Iterator` of rows. Each row is an `Array[String]`
    *
    * @param in            stream to read from
    * @param delimiter     delimiter between columns, auto-detected if missing
    * @param lineSeparator lineSeparator between rows, auto-detected if missing
    * @param codec         codec of the bytes, set to `Codec.UTF8` if missing
    * @return the iterator of rows
    */
  def apply(in: java.io.InputStream,
            delimiter: Option[Char] = None,
            lineSeparator: Option[String] = None,
            codec: scala.io.Codec = scala.io.Codec.UTF8): Iterator[Array[String]] = {
    val reader = new CsvParser(settings(delimiter, lineSeparator))
    // We use Apache commons `BOMInputStream` to manage the presence of a BOM character in your CSV content
    reader.beginParsing(new java.io.InputStreamReader(new BOMInputStream(in), codec.charSet))

    Iterator.iterate(null: Array[String])(_ => reader.parseNext())
      .drop(1) // the first null
      .takeWhile(_ != null)
      .map(row)
  }

  @inline private def row(row: Array[String]): Array[String] = if (row == null) Array() else row

  /** Sets minimum settings that have been tested and work with most csv files
    *
    * @param delimiter     delimiter between columns, auto-detected if missing
    * @param lineSeparator lineSeparator between rows, auto-detected if missing
    * @return the `CsvParserSettings`
    */
  private def settings(delimiter: Option[Char] = None,
                       lineSeparator: Option[String] = None): CsvParserSettings = {
    val settings = new CsvParserSettings()
    delimiter.fold(settings.setDelimiterDetectionEnabled(true))( // detection not enabled by default
      d => settings.getFormat.setDelimiter(d))
    lineSeparator.fold(settings.setLineSeparatorDetectionEnabled(true))( // detection not enabled by default
      ls => settings.getFormat.setLineSeparator(ls))
    settings.setEmptyValue("") // default is null
    settings.setNullValue("") // default is null
    settings.setMaxColumns(65535) // default is 512!
    settings.setMaxCharsPerColumn(65535) // default is 4096

    settings
  }
}
