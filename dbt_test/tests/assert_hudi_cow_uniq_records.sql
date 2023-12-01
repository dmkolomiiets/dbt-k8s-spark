SELECT count(1) cnt
from (SELECT distinct id
      FROM {{ref('hudi_insert_table')}}) c
having count(1) <> (SELECT count(1) from {{ref('hudi_insert_table')}})
