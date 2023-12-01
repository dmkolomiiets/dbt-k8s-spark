{{ config(
    materialized='incremental',
    file_format='hudi',
    incremental_strategy='merge',
    options={
        'type': 'mor',
        'primaryKey': 'id',
        'precombineKey': 'ts',
    },
    unique_key='id',
    partition_by='datestr',
    pre_hook=["set spark.sql.datetime.java8API.enabled=false;"],
   )
}}

select id, name, current_timestamp() as ts, current_date as datestr
from {{ ref('hudi_upsert_table') }}
