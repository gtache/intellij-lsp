package com.github.gtache.client.languageserver.serverdefinition

object UserConfigurableServerDefinition extends UserConfigurableServerDefinitionObject {

  /**
    * Returns the UserConfigurableServerDefinition corresponding to the given GUI fields
    *
    * @param ext       The extension
    * @param path      The path or artifact
    * @param mainClass The mainClass
    * @param args      The arguments
    * @return The server definition
    */
  def fromFields(ext: String, path: String, mainClass: String, args: String): UserConfigurableServerDefinition = {
    if ((mainClass.isEmpty || mainClass == null) && path.endsWith(".exe")) {
      new ExeLanguageServerDefinition(ext, path, args.split(" "))
    }
    else {
      new ArtifactLanguageServerDefinition(ext, path, mainClass, args.split(" "))
    }
  }

  /**
    * Transforms a (java) Map<String, ServerDefinitionExtensionPointArtifact> to a Map<String, String[]>
    *
    * @param map A java map
    * @return the transformed java map
    */
  def toArrayMap(map: java.util.Map[String, UserConfigurableServerDefinition]): java.util.Map[String, Array[String]] = {
    import scala.collection.JavaConverters._
    map.asScala.map(e => (e._1, e._2.toArray)).asJava
  }

  /**
    * Transforms a (java) Map<String, String[]> to a Map<String, ServerDefinitionExtensionPointArtifact>
    *
    * @param map A java map
    * @return the transformed java map
    */
  def fromArrayMap(map: java.util.Map[String, Array[String]]): java.util.Map[String, UserConfigurableServerDefinition] = {
    import scala.collection.JavaConverters._
    map.asScala.map(e => (e._1, fromArray(e._2))).asJava
  }

  override def fromArray(arr: Array[String]): UserConfigurableServerDefinition = {
    val artifact = ArtifactLanguageServerDefinition.fromArray(arr)
    if (artifact == null) {
      ExeLanguageServerDefinition.fromArray(arr)
    } else {
      artifact
    }
  }

  override def typ: String = "userConfigurable"
}

/**
  * A UserConfigurableServerDefinition is a server definition which can be manually entered by the user in the IntellliJ settings
  */
trait UserConfigurableServerDefinition extends LanguageServerDefinition {

  /**
    * @return The array corresponding to the server definition
    */
  def toArray: Array[String]
}
