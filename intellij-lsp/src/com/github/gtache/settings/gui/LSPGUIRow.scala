package com.github.gtache.settings.gui

import javax.swing.{JComboBox, JComponent, JPanel, JTextField}

import com.intellij.openapi.diagnostic.Logger

import scala.collection.mutable

/**
  * Class representing a row in the settings window
  *
  * @param panel  The row as a JPanel
  * @param typ    The typ of the row
  * @param fields The fields of the row
  */
case class LSPGUIRow(panel: JPanel, typ: String, fields: mutable.LinkedHashMap[String, JComponent]) {
  private val LOG: Logger = Logger.getInstance(classOf[LSPGUIRow])

  /**
    * @return the type of the row (Artifact, exe or command)
    */
  def getTyp: String = typ

  /**
    * @param label The label corresponding to the text field
    * @return The content of the text field
    */
  def getText(label: String): String = {
    fields.get(label).fold("") {
      case t: JTextField => t.getText()
      case b: JComboBox[String] => b.getSelectedItem.asInstanceOf[String]
      case u: JComponent => LOG.error("Unknown JComponent : " + u)
        ""
    }
  }

  /**
    * @return A string array representing this row
    */
  def toStringArray: Array[String] = {
    Array(typ) ++ fields.values.map {
      case t: JTextField => t.getText()
      case b: JComboBox[String] => b.getSelectedItem.asInstanceOf[String]
      case u: JComponent => LOG.error("Unknown JComponent : " + u)
        ""
    }
  }
}
