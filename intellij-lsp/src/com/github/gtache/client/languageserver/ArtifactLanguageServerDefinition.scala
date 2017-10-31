package com.github.gtache.client.languageserver

import java.io.{InputStream, OutputStream}

import com.github.gtache.CoursierImpl
import com.github.gtache.client.connection.{ProcessStreamConnectionProvider, StreamConnectionProvider}

/**
  * Represents a ServerDefinition for a LanguageServer stored on a repository
  *
  * @param ext       The extension that the server manages
  * @param packge    The artifact id of the server
  * @param mainClass The main class of the server
  * @param args      The arguments to give to the main class
  */
case class ArtifactLanguageServerDefinition(ext: String, packge: String, mainClass: String, args: Array[String]) extends LanguageServerDefinition {

  override def start(): (InputStream, OutputStream) = {
    streamConnectionProvider.start()
    (streamConnectionProvider.getInputStream, streamConnectionProvider.getOutputStream)
  }


  override def stop(): Unit = {
    streamConnectionProvider.stop()
  }


  override def createConnectionProvider(workingDir: String): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      val cp = CoursierImpl.resolveClasspath(packge)
      streamConnectionProvider = new ProcessStreamConnectionProvider(Seq("java", "-cp", cp, mainClass) ++ args, workingDir)
    }
    streamConnectionProvider
  }

  override def toString: String = super.toString + " artifact : " + packge + " mainClass : " + mainClass + " args : " + args.mkString(" ")
}
