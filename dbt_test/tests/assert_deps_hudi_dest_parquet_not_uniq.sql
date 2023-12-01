SELECT count(1)
FROM (SELECT distinct id from {{ref('deps_hudi_dest_parquet')}}) a
having count(1) = (SELECT count(1) from {{ref('deps_hudi_dest_parquet')}})
