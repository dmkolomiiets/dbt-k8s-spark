{{
    config(

        materialized='table',
        file_format='parquet',
        pre_hook=['SET spark.sql.legacy.allowNonEmptyLocationInCTAS=true', 'SET mapreduce.input.fileinputformat.input.dir.recursive=true']
    )
}}

with source_data as (

    select *
    from {{ ref('plan_claim_header_csv') }}
    union all
    select *
    from {{ ref('plan_claim_header_parquet') }}
), type as (
    select format_number(rand()*1000, 0) as id
    union all
    select format_number(rand()*1000, 0) as id
    union all
    select format_number(rand()*1000, 0) as id
    union all
    select format_number(rand()*1000, 0) as id
    union all
    select format_number(rand()*1000, 0) as id
    union all
    select format_number(rand()*1000, 0) as id
    union all
    select format_number(rand()*1000, 0) as id
    )

select * from source_data, type
