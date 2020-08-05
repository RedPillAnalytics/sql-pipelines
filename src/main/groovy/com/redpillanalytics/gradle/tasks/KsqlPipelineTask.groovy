package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * List all topics available to KSQL
 */
@Slf4j
class KsqlPipelineTask extends PipelineTask {

   KsqlPipelineTask() {
      description = "Execute a Confluent KSQL-based pipeline task."
      group = project.extensions."$EXTENSION".pipelineGroup
   }

   /**
    * The REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.SqlPipelineExtension#pipelineEndpoint}.
    */
   @Input
   @Option(option = "rest-url",
           description = "The REST API URL for the KSQL Server. Default: value of 'confluent.pipelineEndpoint' or 'http://localhost:8088'."
   )
   String restUrl = project.extensions."$EXTENSION".pipelineEndpoint

   /**
    * When defined, the underlying topic is deleted when the table or stream is dropped.
    */
   @Input
   @Option(option = "delete-topic",
           description = "When defined, the underlying topic is deleted when the table or stream is dropped."
   )
   boolean deleteTopic

   /**
    * The Username for Basic Authentication with the REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.SqlPipelineExtension#pipelineUsername}.
    */
   @Input
   @Optional
   @Option(option = "basic-username",
           description = "The Username for Basic Authentication with the REST API URL for the KSQL Server. Default: value of 'confluent.pipelineUsername' or ''."
   )
   String username = project.extensions."$EXTENSION".pipelineUsername

   /**
    * The Password for Basic Authentication with the REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.SqlPipelineExtension#pipelinePassword}.
    */
   @Input
   @Optional
   @Option(option = "basic-password",
           description = "The Password for Basic Authentication with the REST API URL for the KSQL Server. Default: value of 'confluent.pipelinePassword' or ''."
   )
   String password = project.extensions."$EXTENSION".pipelinePassword

   /**
    * Instantiates a KsqlRest Class, which is used for interacting with the KSQL RESTful API.
    *
    * @return {@link com.redpillanalytics.KsqlRest}
    */
   @Internal
   def getKsqlRest() {

      return new KsqlRest(restUrl: restUrl, username: username, password: password)
   }

   /**
    * When defined, then set "ksql.streams.auto.offset.reset" to "earliest".
    */
   @Input
   @Option(option = "from-beginning",
           description = "When defined, set 'ksql.streams.auto.offset.reset' to 'earliest'."
   )
   boolean fromBeginning = false

   /**
    * When defined, applicable TERMINATE statements are not auto-generated and executed.
    */
   @Input
   @Option(option = 'no-terminate',
           description = 'When defined, applicable TERMINATE statements are not auto-generated and executed.'
   )
   boolean noTerminate

   @TaskAction
   def pipelineTask() {

      // drop objects and terminate queries
      if (!noDrop) {
         if (ksqlRest.getSourceDescription(objectName)) {

            // get any persistent queries reading or writing to this table/stream
            List queryIds = ksqlRest.getQueryIds(objectName)

            // terminate persistent queries
            if (!queryIds.isEmpty()) {
               if (!noTerminate) {
                  queryIds.each { query ->
                     logger.info "Terminating query $query..."
                     def result = ksqlRest.execKsql("TERMINATE ${query}")
                  }
               } else log.info "Persistent queries exist, but '--no-terminate' option provided."
            }
         }
         // drop object
         def result = ksqlRest.dropKsql(dropSql, [:])
      }

      // create KSQL objects
      if (!noCreate) {
         def result = ksqlRest.createKsql(compiledSql, fromBeginning)
      }
   }
}
