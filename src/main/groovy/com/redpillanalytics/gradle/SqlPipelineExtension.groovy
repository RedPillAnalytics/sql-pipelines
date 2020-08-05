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
    * Policy for handling dependencies. When 'ordered', dependencies will not be executed unless they already exist in the graph. When 'forced', dependencies will be added to the graph. Default: 'ordered'.
    */
   String dependencyPolicy = 'ordered'

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
      try {
         return new Yaml().load(file.text)
      } catch (FileNotFoundException e) {
         return [:]
      }
   }

   /**
    * Return a Groovy representation of a YAML file with same name as the parent directory.
    *
    * @return The Groovy representation of a YAML file with same name as the parent directory.
    */
   def getParentConfig(File file) {
      def config = getConfig(new File(file.parentFile, "model.yaml"))
      assert !config.name
      assert !config.description
   }

   /**
    * Return a configuration value compared between two configurations, setting a default if both are null.
    *
    * @return A configuration value.
    */
   def getConfigValue(def child, def parent, String property, def defaultValue) {
      def value
      if (child?."$property") {
         log.debug "'${property}' was in the child file."
         value = child?."$property"
      } else if (parent?."$property") {
         log.debug "'${property}' was in the parent file."
         value = parent?."$property"
      } else {
         log.debug "'$property' not found in child or parent. Using default."
         value = defaultValue
      }
      return value
   }

   /**
    * Return a Groovy representation of a YAML file associated with a SQL file.
    *
    * @return The Groovy representation of a YAML configuration
    */
   def getSqlConfig(File file) {
      log.warn "Configuration for: ${file.canonicalPath}."
      Yaml yaml = new Yaml()

      def config = [:], childConfig, parentConfig

      // get the child configuration
      childConfig = getConfig(Utils.getModifiedFile(file, 'yaml'))

      // get the parent configuration
      parentConfig = getParentConfig(file)

      // first lets get the name of the target object
      config.name = getConfigValue(childConfig, parentConfig, "name", Utils.getFileBase(file).replace('-', '_'))

      // first lets get description, which is used for Task description
      config.description = getConfigValue(childConfig, parentConfig, "description", "Create object(s) in '${file.name}'.")

      // schema name
      config.schema = getConfigValue(childConfig, parentConfig, "schema", file.parentFile.name.replace('-', '_'))

      // delete boolean
      config.delete = getConfigValue(childConfig, parentConfig, "delete", 'false').toBoolean()

      // materialization strategy
      config.materialize = getConfigValue(childConfig, parentConfig, "materialize", 'ignore')

      // task grouping
      config.group = getConfigValue(childConfig, parentConfig, "group", pipelineGroup)

      // get dependencies
      def after = getConfigValue(childConfig, parentConfig, "after", [])
      config.after = after.collect{ getTaskName(it)}

      log.debug "Config: ${config}"
      return config
   }
}