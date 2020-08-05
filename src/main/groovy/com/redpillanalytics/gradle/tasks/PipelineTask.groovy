package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

/**
 * List all topics available to KSQL
 */
@Slf4j
class PipelineTask extends DefaultTask {

   static String EXTENSION = 'sql'

   static String SQLREGEX = /(?i)(?:.*)(create|drop|insert)(?:\s+)(table|stream|into)(?:\s+)(?:IF EXISTS\s+)?(\w+)/

   PipelineTask() {
      description = "Execute a SQL-based pipeline task."
      group = project.extensions."$EXTENSION".pipelineGroup
   }

   /**
    * The name of the target object.
    */
   @Input
   String objectName

   /**
    * The original SQL from the source SQL script.
    */
   @Input
   String sourceSql

   /**
    * The method used to materialize the query. Supports 'stream' or 'table' as materializations. Default: 'ignore', which means the materialization is included in the SQL.
    */
   @Input
   String materialize

   /**
    * Returns the compiled SQL.
    *
    * @return compiled SQL.
    */
   @Input
   String getCompiledSql() {

      String sql = ((!materialize == 'ignore') ? "create $materialize" : "") + " " + sourceSql
      sql = sql
              .readLines()
              .findResults { it.replaceAll(~/(\s)*(?:--.*)?/) { all, statement -> (statement ?: '') } }
              .join(' ')
              .replace('  ', '')
              .replaceAll(/\s+/, ' ')
              .replaceAll(/;+/, '')

      log.warn "Compiled: $sql"

      return sql
   }

   /**
    * Returns the DROP SQL statement.
    *
    * @return DROP SQL statement.
    */
   @Input
   String getDropSql() {

      def sql = compiledSql.find(/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
         "DROP $type IF EXISTS ${objectName};\n"
      }

      log.warn "Drop: $sql"

      return sql
   }

   /**
    * When defined, applicable DROP statements are not auto-generated and executed.
    */
   @Input
   @Option(option = 'no-drop',
           description = 'When defined, applicable DROP statements are not auto-generated and executed.'
   )
   boolean noDrop

   /**
    * When defined, CREATE statements found in SQL scripts are not executed. Used primarily for auto-generating and executing applicable DROP statements.
    */
   @Input
   @Option(option = 'no-create',
           description = 'When defined, CREATE statements in KSQL scripts are not executed. Used primarily for auto-generating and executing applicable DROP and/or TERMINATE statements.'
   )
   boolean noCreate

   /**
    * Returns the object type from a SQL CREATE or DROP statement.
    *
    * @return Either 'table' or 'stream' or 'into' (the latter denotes it was an INSERT statement).
    */
   String getObjectName(String sql) {
      return (materialize == 'ignore') ? (sql.find(SQLREGEX) { String all, String statement, String type, String name -> name.toLowerCase() }) : objectName
   }
}
