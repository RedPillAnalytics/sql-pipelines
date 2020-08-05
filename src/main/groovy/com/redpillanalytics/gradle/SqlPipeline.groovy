package com.redpillanalytics.gradle

import com.redpillanalytics.gradle.tasks.KsqlPipelineTask
import com.redpillanalytics.gradle.tasks.ListTopicsTask
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree

@Slf4j
class SqlPipeline implements Plugin<Project> {

   /**
    * Extension name
    */
   static String EXTENSION = 'sql'

   /**
    * Apply the Gradle plugin
    */
   void apply(Project project) {

      // apply Gradle built-in plugins
      project.apply plugin: 'base'
      project.apply plugin: 'com.redpillanalytics.gradle-properties'

      // apply the Gradle plugin extension and the context container
      applyExtension(project)

      // show all topics
      project.task('listTopics', type: ListTopicsTask) {}

      // create deploy task
      project.tasks.register('run') {
         group project.extensions."$EXTENSION".workflowGroup
         description "Execute any configured SQL run tasks."
      }

      // create deploy task
      project.tasks.register('deploy') {
         group project.extensions."$EXTENSION".workflowGroup
         description "Execute any configured SQL deployment tasks."
      }

      project.afterEvaluate {
         // Go look for any -P properties that have "$EXTENSION." in them
         // If so... update the extension value
         project.pluginProps.setParameters(project, EXTENSION)

         // get the location of the source files
         File pipelineDir = project.file(project.extensions."$EXTENSION".getPipelinePath())
         log.warn "pipelineDir: ${pipelineDir.getCanonicalPath()}"

         File pipelineBuildDir = project.file("${project.buildDir}/${project.extensions."$EXTENSION".pipelineBuildName}")
         log.warn "pipelineBuildDir: ${pipelineBuildDir.canonicalPath}"

         File pipelineDeployDir = project.file("${project.buildDir}/${project.extensions."$EXTENSION".pipelineDeployName}")
         log.warn "pipelineDeployDir: ${pipelineDeployDir.canonicalPath}"

         FileTree sqlTree = project.fileTree(dir: pipelineDir, includes: ['**/*.sql', '**/*.SQL', '**/*.ksql', '**/*.KSQL'])

         // create all the tasks
         sqlTree.sort().each { sql ->
            def config = project.extensions."$EXTENSION".getSqlConfig(sql)
            def taskName = project.extensions."$EXTENSION".getTaskName(config.name)
            project.tasks.register(taskName, KsqlPipelineTask) {
               description config.description
               group config.group
               materialize config.materialize
               sourceSql sql.text
               objectName config.name
            }

            project.tasks.run.dependsOn taskName
         }

         // define the ordering
         sqlTree.sort().each { sql ->
            def config = project.extensions."$EXTENSION".getSqlConfig(sql)
            if (config.after) {
               log.warn "Entering after closure..."
               def policy = (project.extensions."$EXTENSION".dependencyPolicy=='forced') ? 'dependsOn' : 'mustRunAfter'
               log.warn "policy: $policy"
               project.tasks."${project.extensions."$EXTENSION".getTaskName(config.name)}"."$policy" config.after
            }
         }
      }
   }

   /**
    * Apply the Gradle Plugin extension.
    */
   void applyExtension(Project project) {

      project.configure(project) {
         extensions.create(EXTENSION, SqlPipelineExtension)
      }
   }
}

