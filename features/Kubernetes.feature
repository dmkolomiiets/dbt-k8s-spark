Feature: Automate Tahoe-ETL testing with Cucumber

  Background: Dependencies are installed
    Given 'kubectl' command is executable
    And 'aws' command is executable
    And 'session-manager-plugin' command is executable
    Then namespace is 'kube-system'

  Scenario: measure-score-export-workflow
    Given namespace is 'argo'
    When I get workflow template
    Then list size is greater then 0
    And contains resource with 'metadata.name' is equal to 'measure-score-export-workflow-template'
    When proxy to argo-service enabled
    And wait 10s for port enabled
    Then I submit workflow with parameters
    """
    {
      "entryPoint": "measure-score-export-workflow",
      "parameters": [
        "customer=arcsm",
        "namespace=dev",
        "host=platform-arcsm-analytics-pg.cofuwcypdiyy.us-east-1.rds.amazonaws.com",
        "event-id=1234",
        "max-spark-executors=16",
        "csv-incremental=true",
        "csv-enabled=false"
      ]
    }
    """
    And It is completed 'Succeeded'
