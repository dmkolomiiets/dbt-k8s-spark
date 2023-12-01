{{
    config(

        materialized='table',
        file_format='parquet',
    )
}}
select id, name, ts, datestr
from {{ ref('hudi_upsert_partitioned_cow_table') }}
union all
select id, name, ts, datestr
from {{ ref('hudi_upsert_partitioned_mor_table') }}
