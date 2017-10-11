ideaBuild in ThisBuild := "172.4343.14" // Released September 26, 2017

// Download the IDEA SDK on startup
onLoad in Global := ((s: State) => { "updateIdea" :: s}) compose (onLoad in Global).value

lazy val root = (project in file(".")).
  enablePlugins(SbtIdeaPlugin). // See https://github.com/JetBrains/sbt-idea-plugin for documentation
  settings(
    name := "intellij-lsp",
    description := "Language Server Protocol plugin for IntelliJ IDEA",
    version := "0.1.0-SNAPSHOT",

    scalaSource       in Compile  := baseDirectory.value / "src",
    scalaSource       in Test     := baseDirectory.value / "test",
    javaSource        in Compile  := baseDirectory.value / "src",
    javaSource        in Test     := baseDirectory.value / "test",
    resourceDirectory in Compile  := baseDirectory.value / "resources",
    resourceDirectory in Test     := baseDirectory.value / "test-resources",

    scalaVersion := "2.12.3",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
    ),

    libraryDependencies ++= Seq(
      "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.3.0",
      "io.get-coursier" %% "coursier" % "1.0.0-RC12",
      "io.get-coursier" %% "coursier-cache" % "1.0.0-RC12",
    ),
    ideaInternalPlugins := Seq(
      "IntelliLang",
    ),
  )
