package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * List all topics available to KSQL
 */
@Slf4j
class PipelineTask extends DefaultTask {

   /**
    * The original SQL from the source SQL script.
    */
   @Input
   String sourceSql

   /**
    * When defined, the underlying topic is deleted when the table or stream is dropped.
    */
   @Input
   @Option(option = "delete-topic",
           description = "When defined, the underlying topic is deleted when the table or stream is dropped."
   )
   boolean deleteTopic

   PipelineTask() {
      description = "Execute a SQL-based pipeline task."
      group = project.extensions.sql.pipelineGroup
   }

   @TaskAction
   def pipelineRun(){
      new KsqlRest().getTopics().each { topic ->
         println "Name: $topic.name, Registered: $topic.registered, Partitions: ${topic.replicaInfo.size()}, Consumers: $topic.consumerCount, Consumer Groups: $topic.consumerGroupCount"
      }
   }
}
