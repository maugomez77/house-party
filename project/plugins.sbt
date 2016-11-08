// for the liquid support plugin
resolvers += "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases")
)(Resolver.ivyStylePatterns)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")

// to generate the sbt gen-idea project for intellij
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// to have the sbt coverage for code basis
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

// to generate the docker images
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")

// for the assembly for runnable jar
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

//jococo test
addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.6")

//sbt release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

//bintray plugin
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.2")

//aether plugin
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.13")

// for war / publish but it is just required to build
addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "1.0.0")

// flyway plugin repo
resolvers += "Flyway" at "https://flywaydb.org/repo"

// flyway plugin
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.3")

