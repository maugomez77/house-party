import com.typesafe.config.ConfigFactory
import scoverage.ScoverageKeys

name := "house-party"

lazy val metricsPlayVersion = "d1a2b66"

lazy val metricsPlay =
  ProjectRef(uri(s"ssh://git@github.com/rocketlawyer/metrics-play.git#$metricsPlayVersion"), "metrics-play")

lazy val swaggerPlayVersion = "8753630"

lazy val swaggerPlay25 =
  ProjectRef(uri(s"ssh://git@github.com/rocketlawyer/swagger-play.git#$swaggerPlayVersion"), "root")

lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(metricsPlay).dependsOn(swaggerPlay25)

disablePlugins(PlayLayoutPlugin)
PlayKeys.playMonitoredFiles ++= (sourceDirectories in(Compile, TwirlKeys.compileTemplates)).value

scalaVersion in ThisBuild := "2.11.8"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "Play2war plugins release" at "http://repository-play-war.forge.cloudbees.com/release/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases/"
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")
resolvers += "Rocketlawyer Snapshots" at "http://f1tst-linbld100/nexus/content/repositories/snapshots"
resolvers += "Rocketlawyer Releases" at "http://f1tst-linbld100/nexus/content/repositories/releases"

libraryDependencies ++= Seq(
  ws,
  jdbc,
  "org.postgresql" % "postgresql" % "9.4.1209",
  filters,
  cache,
  "com.typesafe.play" %% "anorm" % "2.5.0",
  "org.apache.kafka" % "kafka_2.11" % "0.10.0.0",
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "net.cakesolutions" %% "scala-kafka-client" % "0.10.0.0-RC2",
  "net.cakesolutions" %% "scala-kafka-client-akka" % "0.10.0.0-RC2",
  "nl.grons" %% "metrics-scala" % "3.5.4_a2.3",
  "io.prometheus" % "simpleclient" % "0.0.16",
  "io.prometheus" % "simpleclient_hotspot" % "0.0.16",
  "io.prometheus" % "simpleclient_servlet" % "0.0.16",
  "io.prometheus" % "simpleclient_pushgateway" % "0.0.16",
  specs2 % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.0" % "test")

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

// disable .jar publishing
publishArtifact in (Compile, packageBin) := false

//sbtPlugin := true

publishMavenStyle := true

//to remove the reverse files generated from play for the coverage report
ScoverageKeys.coverageExcludedPackages := """controllers\..*Reverse.*;router.Routes.*;kafka\..*.*;"""

//to unable the parallel execution for testing purposes
parallelExecution in Test := false

// for the liquid support

// first read the local file from play application.conf
def getConfig: com.typesafe.config.Config = {
  val classLoader = new java.net.URLClassLoader( Array( new File("./src/main/resources/").toURI.toURL ) )
  ConfigFactory.load(classLoader)
}



// to generate the docker images
enablePlugins(JavaAppPackaging)


// for the standalone jar
assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", "log4j", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.discard
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x if x.endsWith("application.conf") => MergeStrategy.first
  case x if x.endsWith("spring.tooling") => MergeStrategy.first
  case x if x.endsWith("logback.xml") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getName.contains("slf4j-log4j12")}
}

mainClass in assembly := Some("play.core.server.ProdServerStart")


fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)

