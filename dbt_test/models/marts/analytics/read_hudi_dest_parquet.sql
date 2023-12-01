{{
    config(

        materialized='table',
        file_format='parquet',
        pre_hook=['SET spark.sql.legacy.allowNonEmptyLocationInCTAS=true', 'SET mapreduce.input.fileinputformat.input.dir.recursive=true']
    )
}}
select * from {{source('spark_dbt_test', 'hudi_test')}}
