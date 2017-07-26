package com.okune.csv

/** Writes CSV-type data one row at a time until `close()` is called. */
trait CSVWriter extends Function1[Array[String], Unit] with java.io.Closeable

object CSVWriter {

  import com.univocity.parsers.csv._

  def apply(out: java.io.OutputStream,
            delimiter: Char, lineSeparator: String = "\n",
            codec: scala.io.Codec = scala.io.Codec.UTF8): CSVWriter = {
    val writer = new CsvWriter(new java.io.OutputStreamWriter(out, codec.charSet), settings(delimiter, lineSeparator))

    new CSVWriter {
      def apply(row: Array[String]): Unit = writer.writeRow(row)

      def close(): Unit = writer.close
    }
  }

  /** Sets minimum settings that have been tested and work with most csv files
    *
    * @param delimiter     delimiter between columns, auto-detected if missing
    * @param lineSeparator lineSeparator between rows, auto-detected if missing
    * @return the `CsvWriterSettings`
    */
  private def settings(delimiter: Char, lineSeparator: String): CsvWriterSettings = {
    val settings = new CsvWriterSettings()
    settings.getFormat.setLineSeparator(lineSeparator)
    settings.getFormat.setDelimiter(delimiter)
    settings.setQuoteEscapingEnabled(true)
    settings.setQuoteAllFields(true)

    settings
  }
}
