package com.github.gtache.lsp.utils

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.StringReader
import java.io.StringWriter

/**
 * Represents a single CSV line
 * @param csvLine The CSV line already formatted
 */
data class CSVLine(val csvLine: String) {
    constructor(values: Iterable<String>) : this(toCSV(values))

    /**
     * Returns the CSV line as a list of strings
     */
    fun toList(): List<String> {
        val reader = StringReader(csvLine)
        CSVParser(reader, CSVFormat.DEFAULT).use {
            if (it.recordNumber != 1L) {
                throw IllegalArgumentException("Expected only one record for $csvLine, got ${it.records}")
            } else {
                val rec = it.records[0]
                return rec.toList()
            }
        }
    }

    /**
     * Returns the CSV line as a single value, assuming this line only has one value
     */
    fun toSingleString(): String {
        val list = toList()
        if (list.size == 1) {
            return list[0]
        } else {
            throw IllegalArgumentException("Expected only one value for $csvLine, got ${list.joinToString(",")}")
        }
    }

    companion object {
        private fun toCSV(values: Iterable<String>): String {
            val writer = StringWriter()
            CSVPrinter(writer, CSVFormat.DEFAULT).use {
                it.printRecord(values)
            }
            return writer.toString()
        }

        /**
         * Returns a CSVLine of a single [value]
         */
        fun of(value: String): CSVLine {
            return CSVLine(listOf(value))
        }
    }
}