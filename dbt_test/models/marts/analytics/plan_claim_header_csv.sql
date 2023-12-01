{{
    config(

        materialized='table',
        file_format='csv',
        pre_hook=['SET spark.sql.legacy.allowNonEmptyLocationInCTAS=true', 'SET mapreduce.input.fileinputformat.input.dir.recursive=true']
    )
}}
select * from {{source('demo3_acpm3286_origin_qdw_v1', 'plan_claim_header')}}
