{{
    config(

        materialized='table',
        file_format='parquet',
        pre_hook=['SET spark.sql.legacy.allowNonEmptyLocationInCTAS=true', 'SET mapreduce.input.fileinputformat.input.dir.recursive=true']
    )
}}
select *,
       row_number() over (PARTITION BY orig_claim_header_id order by claim_type) row_num
from (

                  select *
                  from {{ ref('plan_claim_header_csv') }}
                  union all
                  select *
                  from {{ ref('plan_claim_header_parquet') }}
              ) data
