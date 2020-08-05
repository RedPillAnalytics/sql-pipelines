import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Check basic configuration")
class RunTest extends Specification {

   @Shared
   File projectDir, buildDir, settingsFile, resourcesDir, buildFile, artifact

   @Shared
   def result, taskList

   @Shared
   String taskName, projectName = 'pipeline-run', options = '-Si'

   @Shared
   String pipelineEndpoint = System.getProperty("pipelineEndpoint") ?: 'http://localhost:8088'

   @Shared
   String kafkaServers = System.getProperty("kafkaServers") ?: 'localhost:9092'

   @Shared
   String analyticsVersion = System.getProperty("analyticsVersion")

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['clean', 'assemble', 'check', 'pipelineScript', 'pipelineZip', 'build']

      resourcesDir = new File('src/test/resources')

      def ant = new AntBuilder()

      ant.delete(dir: projectDir)

      ant.copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

      buildFile = new File(projectDir, 'build.gradle').write("""
               |plugins {
               |  id 'com.redpillanalytics.sql-pipelines'
               |  id "com.redpillanalytics.gradle-analytics" version "$analyticsVersion"
               |  id 'maven-publish'
               |}
               |
               |repositories {
               |  jcenter()
               |  mavenLocal()
               |  maven {
               |     name 'test'
               |     url 'gcs://maven.redpillanalytics.io/demo'
               |  }
               |}
               |
               |sql {
               |  pipelineEndpoint = '$pipelineEndpoint'
               |}
               |analytics {
               |   kafka {
               |     test {
               |        bootstrapServers = '$kafkaServers'
               |     }
               |  }
               |}
               |""".stripMargin())
   }

   // helper method
   def executeSingleTask(String taskName, List otherArgs) {

      otherArgs.add(0, taskName)

      log.warn "runner arguments: ${otherArgs.toString()}"

      // execute the Gradle test build
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments(otherArgs)
              .withPluginClasspath()
              .forwardOutput()
              .build()
   }

   def "Execute :run with defaults"() {
      given:
      taskName = 'run'
      result = executeSingleTask(taskName, [options])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILED')
      result.tasks.collect { it.path - ":" }.containsAll(['pipelineClickstream', 'pipelineCustomerClickstream', 'pipelineEventsPerMin', 'pipelineWebUsers', 'run'])
   }
}
