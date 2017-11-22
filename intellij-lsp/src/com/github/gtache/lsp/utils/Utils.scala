package com.github.gtache.lsp.utils

import com.intellij.openapi.diagnostic.Logger

import scala.annotation.varargs

/**
  * Object containing some useful methods for the plugin
  */
object Utils {

  private val LOG: Logger = Logger.getInstance(Utils.getClass)

  /**
    * Transforms an array into a string (using mkString, useful for Java)
    *
    * @param arr The array
    * @param sep A separator
    * @return The result of mkString
    */
  def arrayToString(arr: Array[Any], sep: String = ""): String = {
    arr.mkString(sep)
  }

  /**
    * Concatenate multiple arrays
    *
    * @param arr The arrays
    * @return The concatenated arrays
    */
  @varargs def concatenateArrays(arr: Array[Any]*): Array[Any] = {
    arr.flatten.toArray
  }


}
