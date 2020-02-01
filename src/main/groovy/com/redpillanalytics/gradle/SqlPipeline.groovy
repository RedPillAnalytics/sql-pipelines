package com.redpillanalytics.gradle

import com.redpillanalytics.gradle.tasks.ListTopicsTask
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

@Slf4j
class SqlPipeline implements Plugin<Project> {

   /**
   * Extension name
   */
   static final String EXTENSION = 'sql'

   /**
    * Apply the Gradle plugin
    */
   void apply(Project project) {

      // apply Gradle built-in plugins
      project.apply plugin: 'base'
      project.apply plugin: 'com.redpillanalytics.gradle-properties'

      // apply the Gradle plugin extension and the context container
      applyExtension(project)

      // get the taskGroup
      String taskGroup = project.extensions."$EXTENSION".taskGroup

      // show all topics
      project.task('listTopics', type: ListTopicsTask) {}

      // create deploy task
      project.task('run') {
         group taskGroup
         description "Execute any configured SQL run tasks."
      }

      // create deploy task
      project.task('deploy') {
         group taskGroup
         description "Execute any configured SQL deployment tasks."
      }

      project.afterEvaluate {
         // Go look for any -P properties that have "$EXTENSION." in them
         // If so... update the extension value
         project.pluginProps.setParameters(project, EXTENSION)
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

