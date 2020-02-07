package com.redpillanalytics.gradle

import com.redpillanalytics.common.Utils
import groovy.util.logging.Slf4j
import org.yaml.snakeyaml.Yaml

@Slf4j
class SqlPipelineExtension {

   /**
    * The group name to use for all Pipeline tasks. Default: 'SQL Pipeline'.
    */
   String pipelineGroup = 'SQL Pipeline'

   /**
    * The group name to use for all other SQL tasks. Default: 'SQL Workflow'.
    */
   String workflowGroup = 'SQL Workflow'

   /**
    * Base source directory for the SQL Pipelines plugin. Default: 'src/main'.
    */
   String sourceBase = 'src/main'

   /**
    * Name of the Pipeline source directory that resides in the {@link #sourceBase} directory. Default: 'pipeline'.
    */
   String pipelineSourceName = 'pipeline'

   /**
    * Full path of the Pipeline source directory. When set, this overrides the values of {@link #sourceBase} and {@link #pipelineSourceName}. Default: null.
    */
   String pipelineSourcePath

   /**
    * The name of the Pipeline build directory in the project build directory. Default: 'pipeline'.
    */
   String pipelineBuildName = 'pipeline'

   /**
    * The name of the Pipeline deploy directory in the project build directory. Default: 'pipeline'.
    */
   String pipelineDeployName = 'pipeline'

   /**
    * The name of the Pipeline deployment 'create' script, which contains all the persistent statements that need to be executed. Default: 'ksql-script.sql'.
    */
   String pipelineCreateName = 'ksql-script.sql'

   /**
    * RESTful endpoint for the KSQL Server. Default: 'http://localhost:8088'.
    */
   String pipelineEndpoint = 'http://localhost:8088'

   /**
    * Username for Basic Authentication with the RESTful endpoint. Default: ''.
    */
   String pipelineUsername

   /**
    * Password for Basic Authentication with the RESTful endpoint. Default: ''.
    */
   String pipelinePassword

   /**
    * The pattern used for matching the pipeline deployment artifact. Default: 'pipeline'.
    */
   String pipelinePattern = 'pipeline'

   /**
    * Provides the path for Pipeline source directory.
    *
    * @return The full path of the Pipeline source directory. Uses {@link #pipelineSourcePath} first if it exists, and if it doesn't (the default), then it uses {@link #sourceBase} and {@link #pipelineSourceName}.
    */
   String getPipelinePath() {
      return (pipelineSourcePath ?: "${sourceBase}/${pipelineSourceName}")
   }

   /**
    * Return a normalized task name for SQL Pipeline tasks.
    *
    * @return the task name.
    */
   def getTaskName(String name) {
      return ("pipeline" + name.replaceAll("(_)([A-Za-z0-9])", { Object[] obj -> obj[2].toUpperCase() }).capitalize())
   }

   /**
    * Return a Groovy representation of a YAML file.
    *
    * @return The Groovy representation of a YAML configuration
    */
   def getConfig(File file) {
      return new Yaml().load(file.text)
   }

   /**
    * Return a Groovy representation of a YAML file associated with a SQL file.
    *
    * @return The Groovy representation of a YAML configuration
    */
   def getSqlConfig(File file) {
      Yaml yaml = new Yaml()
      def config

      try {
         config = getConfig(Utils.getModifiedFile(file, 'yaml'))
      } catch (FileNotFoundException e) {
         config = [:]
      }

      if (!config.name) {
         config.name = Utils.getFileBase(file)
      }

      if (!config.description) {
         config.description = "Default model name ${Utils.getFileBase(file)}"
      }

      if (!config.schema) {
         config.schema = file.parentFile.name
      }

      if (!config.delete) {
         config.delete = false
      }

      log.warn "Config: ${config}"
      return config
   }
}