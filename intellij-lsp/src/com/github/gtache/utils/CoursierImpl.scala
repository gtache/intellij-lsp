package com.github.gtache.utils

import java.io.File

import com.intellij.openapi.diagnostic.Logger
import coursier.Cache
import coursier.maven.MavenRepository

import scalaz.concurrent.Task

/**
  * Coursier is used to fetch the dependencies for a given server package (example : ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2) and returning the classpath to run it using java
  */
object CoursierImpl {


  private val LOG: Logger = Logger.getInstance(CoursierImpl.getClass)

  private val repositories = Seq(Cache.ivy2Local, MavenRepository("https://repo1.maven.org/maven2"))

  /**
    * Downloads the dependencies and returns the classpath for a given package
    *
    * @param toResolve The string
    * @return The full classpath string
    */
  def resolveClasspath(toResolve: String): String = {
    import coursier._
    val start = Resolution(Set({
      val parsed = parseDepString(toResolve)
      Dependency(Module(parsed._1, parsed._2), parsed._3)
    }))
    val fetch = Fetch.from(repositories, Cache.fetch())
    val resolution = start.process.run(fetch).unsafePerformSync
    val localArtifacts = Task.gatherUnordered(resolution.artifacts.map(Cache.file(_).run)).unsafePerformSync
    if (!localArtifacts.forall(_.isRight)) {
      LOG.error("Couldn't fetch all dependencies")
      ""
    } else {
      val cp = localArtifacts.map(f => f.getOrElse(new File(""))).aggregate("")((s, f) => s + File.pathSeparator + f.getAbsolutePath, (s1, s2) => s1 + File.pathSeparator + s2).tail
      LOG.info("Fetched dependencies for " + toResolve)
      cp
    }
  }

  private def parseDepString(str: String): (String, String, String) = {
    val res = str.split(":")
    if (res.length != 3) {
      LOG.error("Couldn't resolve dependency for " + str + " : Unknown format")
      ("", "", "")
    } else {
      (res(0), res(1), res(2))
    }
  }
}
