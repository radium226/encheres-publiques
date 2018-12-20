ThisBuild / organization := "com.github.radium226"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version      := "0.1-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "encheres-publiques",
    libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "3.14.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "1.5.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "1.1.0",
    libraryDependencies += "com.bot4s" % "telegram-core_2.12" % "4.0.0-RC2",
    libraryDependencies += "io.tmos" %% "arm4s" % "1.1.0",
    libraryDependencies += "com.google.guava" % "guava" % "27.0.1-jre",
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    assembly / mainClass := Some("com.github.radium226.encherespubliques.CheckForNewSales")
  )

