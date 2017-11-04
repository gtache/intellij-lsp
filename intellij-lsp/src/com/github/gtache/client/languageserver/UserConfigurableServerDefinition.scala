package com.github.gtache.client.languageserver

object UserConfigurableServerDefinition extends UserConfigurableServerDefinitionObject {
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

trait UserConfigurableServerDefinition extends LanguageServerDefinition {

  def toArray: Array[String]
}
