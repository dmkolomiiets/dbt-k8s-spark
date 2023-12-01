from dbt.cli.main import dbtRunner, dbtRunnerResult
import os
import sys
import inspect

writepath = 'run.txt'

command = sys.argv[1]

mode = 'a' if os.path.exists(writepath) else 'w'
if not os.path.exists(writepath):
    with open(writepath, mode) as f:
        f.write('run')
    dbt = dbtRunner()
    args = ["--debug", "--project-dir", "/dbt_test/", "--profiles-dir", "/dbt_test/", "--threads", '10']
    cli_args = None
    cli_args = [command] + args
    if command == 'external':
        cli_args = ["run-operation", "stage_external_sources"] + args
    elif command == 'python_model':
        cli_args =['run'] + args + ["--select", "python_model"]
    print(cli_args)
    res: dbtRunnerResult = dbt.invoke(cli_args)
else:
    print(inspect.stack())
    print('Already runinig')
dbt
