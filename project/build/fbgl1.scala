import sbt._

class fbgl1(info: ProjectInfo) extends DefaultProject(info) {

  object Repositories {
    lazy val EmbeddedRepo         = MavenRepository("Embedded Repo", (info.projectPath / "embedded-repo").asURL.toString)
    lazy val LocalMavenRepo       = MavenRepository("Local Maven Repo", (Path.userHome / ".m2" / "repository").asURL.toString)
    lazy val LocalIvyRepo         = MavenRepository("Local Ivy Repo", (Path.userHome / ".ivy2" / "local").asURL.toString)
    lazy val AkkaRepo             = MavenRepository("Akka Repository", "http://scalablesolutions.se/akka/repository")
    lazy val CodehausRepo         = MavenRepository("Codehaus Repo", "http://repository.codehaus.org")
    lazy val GuiceyFruitRepo      = MavenRepository("GuiceyFruit Repo", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
    lazy val JBossRepo            = MavenRepository("JBoss Repo", "http://repository.jboss.org/nexus/content/groups/public/")
    lazy val JavaNetRepo          = MavenRepository("java.net Repo", "http://download.java.net/maven/2")
    lazy val SonatypeSnapshotRepo = MavenRepository("Sonatype OSS Repo", "http://oss.sonatype.org/content/repositories/releases")
    lazy val GlassfishRepo        = MavenRepository("Glassfish Repo", "http://download.java.net/maven/glassfish")
    lazy val ScalaToolsRelRepo    = MavenRepository("Scala Tools Releases Repo", "http://scala-tools.org/repo-releases")
    lazy val DatabinderRepo       = MavenRepository("Databinder Repo", "http://databinder.net/repo")

    lazy val CasbahRepo           = MavenRepository("Casbah Repo", "http://repo.bumnetworks.com/releases")
    lazy val CasbahSnapshotRepo   = MavenRepository("Casbah Snapshots", "http://repo.bumnetworks.com/snapshots")
    lazy val FusesourceSnapshotRepo = MavenRepository("Fusesource Snapshots", "http://repo.fusesource.com/nexus/content/repositories/snapshots")
    lazy val SunJDMKRepo          = MavenRepository("Sun JDMK Repo", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo")
    lazy val CasbahRepoReleases   = MavenRepository("Casbah Release Repo", "http://repo.bumnetworks.com/releases")
    lazy val ZookeeperRepo        = MavenRepository("Zookeeper Repo", "http://lilycms.org/maven/maven2/deploy/")
    lazy val ClojarsRepo          = MavenRepository("Clojars Repo", "http://clojars.org/repo")
    lazy val TerrastoreRepo       = MavenRepository("Terrastore Releases Repo", "http://m2.terrastore.googlecode.com/hg/repo")
    lazy val MsgPackRepo          = MavenRepository("Message Pack Releases Repo","http://msgpack.sourceforge.net/maven2/")
  }

  // -------------------------------------------------------------------------------------------------------------------
  // ModuleConfigurations
  // Every dependency that cannot be resolved from the built-in repositories (Maven Central and Scala Tools Releases)
  // must be resolved from a ModuleConfiguration. This will result in a significant acceleration of the update action.
  // Therefore, if repositories are defined, this must happen as def, not as val.
  // -------------------------------------------------------------------------------------------------------------------

  import Repositories._
  lazy val embeddedRepo            = EmbeddedRepo // This is the only exception, because the embedded repo is fast!
  lazy val localMavenRepo          = LocalMavenRepo // Second exception, also fast! ;-)
  lazy val localIvyRepo            = LocalIvyRepo // Second exception, also fast! ;-)
  lazy val jettyModuleConfig       = ModuleConfiguration("org.eclipse.jetty", sbt.DefaultMavenRepository)
  lazy val guiceyFruitModuleConfig = ModuleConfiguration("org.guiceyfruit", GuiceyFruitRepo)
  lazy val glassfishModuleConfig   = ModuleConfiguration("org.glassfish", GlassfishRepo)
  lazy val jbossModuleConfig       = ModuleConfiguration("org.jboss", JBossRepo)
  lazy val jerseyContrModuleConfig = ModuleConfiguration("com.sun.jersey.contribs", JavaNetRepo)
  lazy val jerseyModuleConfig      = ModuleConfiguration("com.sun.jersey", JavaNetRepo)
  lazy val multiverseModuleConfig  = ModuleConfiguration("org.multiverse", CodehausRepo)
  lazy val nettyModuleConfig       = ModuleConfiguration("org.jboss.netty", JBossRepo)
  lazy val scalaTestModuleConfig   = ModuleConfiguration("org.scalatest", ScalaToolsRelRepo)
  lazy val logbackModuleConfig     = ModuleConfiguration("ch.qos.logback", sbt.DefaultMavenRepository)
  lazy val spdeModuleConfig        = ModuleConfiguration("us.technically.spde", DatabinderRepo)
  lazy val processingModuleConfig  = ModuleConfiguration("org.processing", DatabinderRepo)

  lazy val jdmkModuleConfig        = ModuleConfiguration("com.sun.jdmk", SunJDMKRepo)
  lazy val jmsModuleConfig         = ModuleConfiguration("javax.jms", SunJDMKRepo)
  lazy val jmxModuleConfig         = ModuleConfiguration("com.sun.jmx", SunJDMKRepo)
  lazy val atomikosModuleConfig    = ModuleConfiguration("com.atomikos",sbt.DefaultMavenRepository)
  lazy val casbahRelease           = ModuleConfiguration("com.novus",CasbahRepoReleases)
  lazy val zookeeperRelease        = ModuleConfiguration("org.apache.hadoop.zookeeper",ZookeeperRepo)
  lazy val casbahModuleConfig      = ModuleConfiguration("com.novus", CasbahRepo)
  lazy val timeModuleConfig        = ModuleConfiguration("org.scala-tools", "time", CasbahSnapshotRepo)
  lazy val voldemortModuleConfig   = ModuleConfiguration("voldemort", ClojarsRepo)
  lazy val terrastoreModuleConfig  = ModuleConfiguration("terrastore", TerrastoreRepo)
  lazy val msgPackModuleConfig     = ModuleConfiguration("org.msgpack", MsgPackRepo)
  lazy val resteasyModuleConfig    = ModuleConfiguration("org.jboss.resteasy", JBossRepo)
  lazy val jsr166yModuleConfig     = ModuleConfiguration("jsr166y", TerrastoreRepo)
  lazy val args4jModuleConfig      = ModuleConfiguration("args4j", JBossRepo)
  lazy val scannotationModuleConfig= ModuleConfiguration("org.scannotation", JBossRepo)

  val AKKA_VERSION = "1.1-SNAPSHOT"
  val LEELOO_VERSION = "0.1"


  // -------------------------------------------------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------------------------------------------------

  object Dependencies {

    // Compile
    lazy val akka_actor =   "se.scalablesolutions.akka"   % "akka-actor"    % AKKA_VERSION % "compile"
    lazy val akka_remote =  "se.scalablesolutions.akka"   % "akka-remote"   % AKKA_VERSION % "compile"
    lazy val akka_http =    "se.scalablesolutions.akka"   % "akka-http"     % AKKA_VERSION % "compile"
    lazy val akka_kernel =  "se.scalablesolutions.akka"   % "akka-kernel"   % AKKA_VERSION % "compile"
    lazy val leeloo_oauth_client = "net.smartam.leeloo" % "oauth2-client" % LEELOO_VERSION % "compile"
  }



  val akkaActor     = Dependencies.akka_actor
  val akkaRemote    = Dependencies.akka_remote
  val akkaHttp      = Dependencies.akka_http
  val akkaKernel    = Dependencies.akka_kernel
  val oauth2Client  = Dependencies.leeloo_oauth_client
}
