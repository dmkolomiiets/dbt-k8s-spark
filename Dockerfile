FROM ubuntu:bionic as downloader

ARG HADOOP_VERSION=3.3.2
ARG AWS_SDK_BUNDLE_VERSION=1.11.901

RUN apt-get update && apt-get install -y \
  wget \
  && rm -rf /var/lib/apt/lists/*

RUN wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_BUNDLE_VERSION}/aws-java-sdk-bundle-${AWS_SDK_BUNDLE_VERSION}.jar -P /tmp/spark-jars/

ENV MSSQL 1.3.0-BETA
ENV MSSQL_JDBC 8.4.1.jre11
ENV HADOOP_VERSION 3.3.4
ENV SPARK_REDSHIFT_VERSION 6.0.0-spark_3.3
ENV REDSHIFT_JDBC_VERSION 2.1.0.19
ENV SPARK_AVRO_VERSION 3.0.1
ENV APP_VERSION 1.0
ENV HUDI_VERSION 0.12.2
ENV AWS_SDK_VERSION 1.11.797

RUN #wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-common/${HADOOP_VERSION}/hadoop-common-${HADOOP_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/org/apache/spark/spark-avro_2.12/${SPARK_AVRO_VERSION}/spark-avro_2.12-${SPARK_AVRO_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/org/apache/hudi/hudi-spark3-bundle_2.12/${HUDI_VERSION}/hudi-spark3-bundle_2.12-${HUDI_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/org/apache/hudi/hudi-aws/${HUDI_VERSION}/hudi-aws-${HUDI_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/org/apache/hudi/hudi-utilities-slim-bundle_2.12/${HUDI_VERSION}/hudi-utilities-slim-bundle_2.12-${HUDI_VERSION}.jar -P /tmp/spark-jars/
RUN wget https://repo1.maven.org/maven2/org/apache/hudi/hudi-spark3.3-bundle_2.12/${HUDI_VERSION}/hudi-spark3.3-bundle_2.12-${HUDI_VERSION}.jar -P /tmp/spark-jars/

FROM apache/spark-py:v3.3.2

COPY --from=public.ecr.aws/dataminded/spark-k8s-glue:v3.3.2-hadoop-3.3.4-v2 $SPARK_HOME/jars/ $SPARK_HOME/jars/
COPY --from=downloader /tmp/spark-jars/* $SPARK_HOME/jars/

USER root

COPY dbt_test/requirements.txt /
RUN python3 -m pip install -r /requirements.txt
RUN python3 -m pip install dbt-core==1.7.2 dbt-spark==1.7.1

ENV DBT_PROFILES_DIR='/dbt_test/'
ENV FAL_STATS_ENABLED=0
ENV DO_NOT_TRACK=1
ENV DBT_LOG_PATH='dbt_logs'
ENV DBT_TARGET_PATH='dbt_target'
COPY dbt_test/ /dbt_test
RUN cd /dbt_test && dbt deps

