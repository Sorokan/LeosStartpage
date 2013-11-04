name := "MyStartpage"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "io.argonaut" %% "argonaut" % "6.0.1",
  "org.jasypt" % "jasypt" % "1.9.1"
)     

play.Project.playScalaSettings
