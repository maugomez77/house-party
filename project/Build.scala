package com.rocketlawyer
package sbtcore

import sbt._
import sbt.plugins._
import Keys._

import de.johoop.jacoco4sbt.JacocoPlugin._

import sbtrelease.ReleasePlugin.autoImport._
import ReleaseTransformations._

import com.earldouglas.xwp.XwpPlugin._

//import Dependencies._

object Testing extends BasicPlugin() {
  val itFilter: String => Boolean = (fileName =>
    (fileName endsWith "IT") ||
      (fileName endsWith "IntegrationTest") ||
      (fileName endsWith "FT") ||
      (fileName endsWith "FunctionalTest") ||
      (fileName endsWith "Spec") ||
      (fileName endsWith "Specs")
    )
  val unitFilter: String => Boolean = !itFilter(_)
  override val customizations = Seq(
    //libraryDependencies ++= Seq(
      //junitInterface % Test,
      //scalaTest % Test,
      //junit % Test
    //),
    testOptions in IntegrationTest := Seq(Tests.Filter(itFilter)),
    testOptions := Seq(Tests.Filter(unitFilter))
  ) ++ addCommandAlias("integrationTest", "it:test")
}

object Coverage extends BasicPlugin(jacoco.settings, Testing)(
  (classDirectory in Compile) := {
    val prev = (classDirectory in Compile).value
    prev.getParentFile / "covered-classes"
  }
  //libraryDependencies += jacocoAgent
)

object NamePreservation extends BasicPlugin(
  moduleName := name.value // sbt, don't change my names
)

object Repos extends BasicPlugin(bintray.Plugin.bintrayResolverSettings)() {
  val internalReleases = "Internal Releases" at "http://f1tst-linbld100/nexus/content/repositories/releases"
  val internalSnapshots = "Internal Snapshots" at "http://f1tst-linbld100/nexus/content/repositories/snapshots"
  val thirdParty = "Third Party Artifacts" at "http://f1tst-linbld100/nexus/content/repositories/thirdparty"
  override val customizations = Seq(
    resolvers ++= Seq( // note: sbt boots with some resolvers
      internalReleases,
      internalSnapshots,
      thirdParty,
      "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
      "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "jcenter" at "http://oss.jfrog.org/artifactory/oss-release-local/"
    )
  )
}

object Publication extends BasicPlugin(aether.Aether.aetherPublishSettings)(
  publishTo := {
    if (isSnapshot.value) Some(Repos.internalSnapshots)
    else Some(Repos.internalReleases)
  },
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in (Compile, packageBin) := true,
  publishArtifact in (Compile, packageSrc) := true,
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in (Test, packageBin) := true,
  publishArtifact in (Test, packageSrc) := true,
  publishArtifact in (Test, packageDoc) := false
)

object PublishDocs extends BasicPlugin(Publication)(
  publishArtifact in (Compile, packageDoc) := true
)

object CompilerSettings extends BasicPlugin(
  crossPaths := false,
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-language:_",
    "-target:jvm-1.8",
    "-deprecation"
  ),
  javacOptions ++= Seq(
    "-source", "1.8",
    "-target", "1.8"
  )
)

object ScmHelpers extends BasicPlugin() {
  object autoImport {
    sealed trait RepoLocation
    case object Github extends RepoLocation

    def rlRepo(location: RepoLocation, name: String) = scmInfo in ThisBuild := (location match {
      case Github =>
        Some(ScmInfo(
          browseUrl = url(s"https://github.com/rocketlawyer/$name"),
          connection = s"scm:git:https://github.com/rocketlawyer/$name.git",
          devConnection = Some(s"scm:git:git@github.com:rocketlawyer/$name.git")
        ))
      case _ => sys.error(s"Unknown repo location for $name ($location)")
    })
  }
}

object SlimReleases extends BasicPlugin(
  javacOptions += (if (isSnapshot.value) "-g:source,lines,vars" else "-g:source,lines"),
  scalacOptions += (if (isSnapshot.value) "-g:vars" else "-g:line")
)

object Release extends BasicPlugin(sbtrelease.ReleasePlugin)() {
  val jacocoTest: ReleaseStep = ReleaseStep(
    action = { st: State =>
      import de.johoop.jacoco4sbt.JacocoPlugin._
      if (!st.get(ReleaseKeys.skipTests).getOrElse(false)) {
        val extracted = Project.extract(st)
        val ref = extracted.get(thisProjectRef)
        extracted.runAggregated(jacoco.check in jacoco.Config in ref, st)
      } else st
    },
    enableCrossBuild = false
  )
  override val customizations = Seq(
    releaseVcs := Some(new JenkinsAwareGit(baseDirectory.value)),
    releaseTagName := s"${name.value}-${version.value}",
    releaseCommitMessage := {
      if (isSnapshot.value) s"Setting version to ${version.value} for next development iteration"
      else s"Setting version to ${version.value} for release"
    },
    releaseVersion := {
      val overridding = Option(System.getenv("Override Release Version")).filter(_.nonEmpty)
      overridding.map(o => (_: String) => o).getOrElse(releaseVersion.value)
    },
    releaseNextVersion := {
      val overridding = Option(System.getenv("Override Next Version")).filter(_.nonEmpty)
      overridding.map { o => (_: String) =>
        if (o.contains("-SNAPSHOT")) o
        else s"$o-SNAPSHOT"
      }.getOrElse(releaseNextVersion.value)
    },
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      jacocoTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}

object OrganizationSettings extends BasicPlugin() {
  override val buildSettings = Seq(
    organization := "com.rocketlawyer"
  )
}

object JenkinsSettings extends BasicPlugin() {
  override val globalSettings = {
    val isRelease = Option(System.getenv("RELEASE_BUILD")).map(_.toBoolean).getOrElse(false)
    if (isRelease) addCommandAlias("jenkins", ";clean;release with-defaults")
    else addCommandAlias("jenkins", ";clean;jacoco:check;publish")
  }
}

// Probably belongs in each person's global...
object Prompt extends BasicPlugin(
  shellPrompt := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }
)

object Library extends ArchitypePlugin() {
  object autoImport {
    val ScalaLibrary = Library
    val JavaLibrary = Library
  }
}

object Webapp extends ArchitypePlugin(tomcat())(
  libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0" % Provided,
  webInfClasses in webapp := true
) {
  object autoImport {
    val ScalaWebapp = Webapp
    val JavaWebapp = Webapp
  }
}

object Runnable extends ArchitypePlugin() {
  object autoImport {
    val ScalaRunnable = Runnable
    val JavaRunnable = Runnable
  }
}

object StructuralProject extends ArchitypePlugin() {
}

class JenkinsAwareGit(baseDir: java.io.File) extends sbtrelease.Git(baseDir) {

  private sealed trait UpstreamSource
  private case object Jenkins extends UpstreamSource
  private case object Filesystem extends UpstreamSource

  private lazy val trackingBranchCmd = cmd("config", "branch.%s.merge" format currentBranch)

  private def upstreamSource: UpstreamSource = {
    if (Option(System.getenv("GIT_BRANCH")).isDefined) Jenkins
    else Filesystem
  }

  private def trackingBranch = upstreamSource match {
    case Jenkins => System.getenv("GIT_BRANCH").split("/", 2)(1)
    case Filesystem => (trackingBranchCmd !!).trim.stripPrefix("refs/heads/")
  }

  override def hasUpstream: Boolean = upstreamSource match {
    case Jenkins => true
    case Filesystem => super.hasUpstream
  }

  override def trackingRemote: String = upstreamSource match {
    case Jenkins => System.getenv("GIT_BRANCH").split("/", 2)(0)
    case Filesystem => super.trackingRemote
  }

  override def isBehindRemote =
    (cmd("rev-list", "%s..%s/%s".format(currentBranch, trackingRemote, trackingBranch)) !! devnull).trim.nonEmpty

  override def currentBranch: String = upstreamSource match {
    case Jenkins => "HEAD"
    case Filesystem => super.currentBranch
  }

  override def pushChanges = upstreamSource match {
    case Jenkins => pushHead #&& pushTags
    case Filesystem => super.pushChanges
  }

  private def pushHead =
    cmd("push", trackingRemote, "%s:%s" format ("HEAD", trackingBranch))

  private def pushTags = cmd("push", "--tags", trackingRemote)

}

class BasicPlugin(
                   baseSettings: Seq[Setting[_]],
                   pluginDeps: Plugins
                   )(_customizations: Setting[_]*) extends AutoPlugin {

  def this(customizations: Setting[_]*) =
    this(Seq.empty, plugins.JvmPlugin)(customizations: _*)

  def this(pluginDeps: Plugins)(customizations: Setting[_]*) =
    this(Seq.empty, pluginDeps)(customizations: _*)

  def this(baseSettings: Seq[Setting[_]])(customizations: Setting[_]*) =
    this(baseSettings, plugins.JvmPlugin)(customizations: _*)

  def customizations = _customizations

  override def requires = pluginDeps

  override def trigger = allRequirements

  override def projectSettings = baseSettings ++ customizations

}

class ArchitypePlugin(
                       baseSettings: Seq[Setting[_]],
                       pluginDeps: Plugins
                       )(_customizations: Setting[_]*) extends AutoPlugin {
  def this(customizations: Setting[_]*) =
    this(Seq.empty, plugins.JvmPlugin)(customizations: _*)

  def this(pluginDeps: Plugins)(customizations: Setting[_]*) =
    this(Seq.empty, pluginDeps)(customizations: _*)

  def this(baseSettings: Seq[Setting[_]])(customizations: Setting[_]*) =
    this(baseSettings, plugins.JvmPlugin)(customizations: _*)

  def customizations = _customizations

  override def requires = pluginDeps

  override def trigger = noTrigger

  override def projectSettings = baseSettings ++ customizations
}