from pyspark.sql import SparkSession
from pyspark import SparkConf
import os
from pyspark.sql.functions import lit

HADOOP_VERSION = '3.3.2'
path='s3a://demo3.data.platform.arcadia/tabular/spark-warehouse'

jars = [
    # f'org.apache.hadoop:hadoop-aws:{HADOOP_VERSION}',
    "org.apache.hudi:hudi-spark3-bundle_2.12:0.12.2",
    "org.apache.hudi:hudi-aws:0.12.2",
    "org.apache.hudi:hudi-utilities-slim-bundle_2.12:0.12.2",
    "org.apache.hudi:hudi-spark3.3-bundle_2.12:0.12.2"
    "com.amazonaws:aws-java-sdk:1.12.429",
]

conf = SparkConf().setAll([
    ('spark.jars.packages', ','.join(jars)),
    # ('spark.hadoop.fs.s3a.aws.credentials.provider', 'com.amazonaws.auth.WebIdentityTokenCredentialsProvider'),
    # ('spark.hadoop.fs.s3a.path.style.access', True),
    ("spark.serializer", "org.apache.spark.serializer.KryoSerializer"),
    ("spark.sql.hive.convertMetastoreParquet", "false"),
    ("hive.metastore.client.factory.class", "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"),
    ("spark.hive.metastore.client.factory.class", "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"),
    ("hive.imetastoreclient.factory.class", "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"),
    ("spark.hive.imetastoreclient.factory.class", "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"),
    ("spark.hadoop.hive.metastore.client.factory.class", "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"),

    ("spark.sql.catalogImplementation", "hive"),

    ])
spark = (
    SparkSession.builder
    .appName("Hudi_Data_Processing_Framework")
    .config(conf=conf)
    .enableHiveSupport()
    .getOrCreate()
)

log4jLogger = spark._jvm.org.apache.log4j
LOGGER = log4jLogger.LogManager.getLogger(__name__)
LOGGER.info("pyspark script logger initialized")
LOGGER.info(os.environ)

input_df = spark.createDataFrame(
    [
        ("100", "2015-01-01", "2015-01-01T13:51:39.340396Z"),
        ("101", "2015-01-01", "2015-01-01T12:14:58.597216Z"),
        ("102", "2015-01-01", "2015-01-01T13:51:40.417052Z"),
        ("103", "2015-01-01", "2015-01-01T13:51:40.519832Z"),
        ("104", "2015-01-02", "2015-01-01T12:15:00.512679Z"),
        ("105", "2015-01-02", "2015-01-01T13:51:42.248818Z"),
    ],
    ("id", "creation_date", "last_update_time"),
)

hudi_options = {
    'hoodie.datasource.write.storage.type': 'COPY_ON_WRITE',
    'hoodie.datasource.write.operation': 'upsert',
    'hudi.metadata-listing-enabled':'true',
    'path': path,
    'hoodie.datasource.hive_sync.enable': 'true',
    'hoodie.datasource.hive_sync.database': 'spark_dbt_test',
    'hoodie.datasource.hive_sync.table': "hudi_test",
    'hoodie.datasource.hive_sync.partition_extractor_class': 'org.apache.hudi.hive.MultiPartKeysValueExtractor',
    'hoodie.datasource.hive_sync.use_jdbc': 'false',
    'hoodie.datasource.hive_sync.mode': 'hms',
    # 'hoodie.meta.sync.client.tool.class': 'org.apache.hudi.aws.sync.AwsGlueCatalogSyncTool',
    # 'hoodie.datasource.hive_sync.database': 'spark_dbt_test',
    # 'hoodie.datasource.hive_sync.table': 'score_json',
    # 'hoodie.datasource.hive_sync.enable': 'true',
    'hoodie.datasource.hive_sync.auto_create_database': 'true',

    'hoodie.table.cdc.enabled':'true',
    'hoodie.table.cdc.supplemental.logging.mode': 'data_before_after',

    "hoodie.table.name": "hudi_test",
    "hoodie.datasource.write.table.name": 'hudi_test',
    "hoodie.datasource.write.recordkey.field": "id",
    "hoodie.datasource.write.precombine.field": "last_update_time",
    "hoodie.datasource.write.partitionpath.field": "creation_date",
    "hoodie.datasource.write.hive_style_partitioning": "true",
    "hoodie.upsert.shuffle.parallelism": 1,
    "hoodie.insert.shuffle.parallelism": 1,
    "hoodie.consistency.check.enabled": True,
    "hoodie.index.type": "BLOOM",
    "hoodie.index.bloom.num_entries": 60000,
    "hoodie.index.bloom.fpp": 0.000000001,
    "hoodie.cleaner.commits.retained": 2,
}
spark.sql("use demo3_inspec")
# spark.sql("show databases").show()

LOGGER.info("hudi start saved")
# INSERT
(
    input_df.write.format("org.apache.hudi")
    .options(**hudi_options)
    .mode("append")
    .save()
)
LOGGER.info("hudi saved")
LOGGER.info("hudi start updated")
# UPDATE
update_df = input_df.limit(1).withColumn("creation_date", lit("2014-01-01"))
(
    update_df.write.format("org.apache.hudi")
    .options(**hudi_options)
    .mode("append")
    .save()
)
LOGGER.info("updated")
LOGGER.info("hudi start updated2")
# REAL UPDATE
update_df = input_df.limit(1).withColumn("last_update_time", lit("2016-01-01T13:51:39.340396Z"))
(
    update_df.write.format("org.apache.hudi")
    .options(**hudi_options)
    .mode("append")
    .save()
)
LOGGER.info("updated2")

output_df = spark.read.format("org.apache.hudi").load(
    f"{path}/*/*"
)
LOGGER.info("before show")


output_df.show()
LOGGER.info("after show")
