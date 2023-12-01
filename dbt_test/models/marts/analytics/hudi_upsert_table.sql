{{ config(
    materialized='incremental',
    file_format='hudi',
    incremental_strategy='merge',
    options={
        'type': 'cow',
        'primaryKey': 'id',
        'precombineKey': 'ts',
    },
    unique_key='id',
   )
}}

select id, name, current_timestamp() as ts
from {{ ref('hudi_insert_overwrite_table') }}
