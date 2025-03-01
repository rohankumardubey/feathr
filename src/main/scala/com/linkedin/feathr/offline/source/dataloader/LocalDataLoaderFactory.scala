package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.util.LocalFeatureJoinUtils
import org.apache.spark.sql.SparkSession

/**
 * DataLoaderFactory for local test environment. It creates a data loader based on the input file type.
 * @param ss the spark session.
 */
private[offline] class LocalDataLoaderFactory(ss: SparkSession) extends DataLoaderFactory {

  private val TEST_AVRO_JSON_FILE = "/data.avro.json"

  /**
   * create a data loader based on the file type.
   *
   * @param path the input file path
   * @return a [[DataLoader]]
   */
  override def create(path: String): DataLoader = {
    log.info(s"Creating local data loader for path: ${path}")
    val workspaceDir = ss.conf.getOption("mockdata_dir")
    if (path.endsWith(".csv")) {
      LocalFeatureJoinUtils.getMockPathIfExist(path, ss.sparkContext.hadoopConfiguration, workspaceDir) match {
        case Some(mockData) => {
          new CsvDataLoader(ss, mockData)
        }
        case None => new CsvDataLoader(ss, path)
      }
    } else if (path.endsWith(".parquet")) {
      LocalFeatureJoinUtils.getMockPathIfExist(path, ss.sparkContext.hadoopConfiguration, workspaceDir) match {
        case Some(mockData) => {
          new ParquetDataLoader(ss, mockData)
        }
        case None => new ParquetDataLoader(ss, path)
      }
    } else if (path.endsWith(".avro.json")) {
      new AvroJsonDataLoader(ss, path)
    } else if (path.startsWith("jdbc")){
      LocalFeatureJoinUtils.getMockPathIfExist(path, ss.sparkContext.hadoopConfiguration, workspaceDir) match {
        case Some(mockData) => {
          new JdbcDataLoader(ss, mockData)
        }
        case None => new JdbcDataLoader(ss, path)
      }
    } else if (getClass.getClassLoader.getResource(path + TEST_AVRO_JSON_FILE) != null) {
      new AvroJsonDataLoader(ss, path + TEST_AVRO_JSON_FILE)
    } else {
      // check if the mock data exist or not, if it does, load the mock data
      LocalFeatureJoinUtils.getMockPathIfExist(path, ss.sparkContext.hadoopConfiguration, workspaceDir) match {
        case Some(mockData) => new JsonWithSchemaDataLoader(ss, mockData)
        case None => new SparkDataLoader(ss, SimplePath(path))
      }
    }
  }
}
