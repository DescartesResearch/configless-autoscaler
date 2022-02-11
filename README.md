# Autoscaler

Autoscaler for autoscaling in CloudFoundry environment. It has a reactive, proactive and safety component. It
imports measurement data from Prometheus. 

## Prerequisites

Required: `java`, `mvn`

## Get it running

(1) Configuration

Set params in `application.yaml`. See Section Configuration.

(2) Build the project

`mvn clean package`


(3) Start the autoscaler

```
cd target
java -jar configless-autoscaler-SNAPSHOT-0.0.1.jar
```

After this step, the autoscaler is an idle mode or if autostart is true, it is already running. 

(4) Access the scaling dashboard via `localhost:8080`

(5) Start autoscaler (if not autostart)

Send a GET request to `localhost:8080/start`

## Configuration 

| Name                                             | Meaning                                                                                                                                                                                                                                                        |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| cloudfoundry.domain                              | Domain of the CF instance (without api. prefix)                                                                                                                                                                                                                |
| cloudfoundry.organization                        | Organization to target in CloudFoundry                                                                                                                                                                                                                         |
| cloudfoundry.space                               | Space to target in CloudFoundry                                                                                                                                                                                                                                |
| cloudfoundry.user                                | Cloudfoundry-user with scaling privileges                                                                                                                                                                                                                      |
| cloudfoundry.password                            | Password for the Cloudfoundry-user account                                                                                                                                                                                                                     |
| cloudfoundry.includeappregex                     | Apps which should be scaled must match this regex                                                                                                                                                                                                              |
| cloudfoundry.excludeappregex                     | Apps which match includeappregex but should not be scaled match this regex                                                                                                                                                                                     |
| prometheus.url                                   | URL of the Prometheus Monitoring Server                                                                                                                                                                                                                        |
| prometheus.scrapeduration                        | Scrape Duration of the Prometheus Monitoring Server                                                                                                                                                                                                            |
| constraints.rules                                | A list containing lower and upper bounds for the number of instances (each entry contains an app (STRING), relation (SMALLER_EQUAL, SMALLER, GREATER, GREATER_EQUAL) and a value (int))                                                                        |
| metrics.eureka-to-cloudfoundry-names             | (Use this if Eureka instance has other app names than CloudFoundry) A map containing Eureka app names as keys and CloudFoundry names as values                                                                                                                 |
| scaling.autostart                                | true if Scaler should start directly, false for manual start (see above)                                                                                                                                                                                       |
| scaling.timeout                                  | Time between two scaling cycles (in seconds)                                                                                                                                                                                                                   |
| scaling.instances                                | The name of the metric containing the number of instances                                                                                                                                                                                                      |
| scaling.upscale.limit                            | Maximum number of instances which can be added with one upscaling decision                                                                                                                                                                                     |
| scaling.upscale.timeout                          | Time between two upscaling decisions (in seconds)                                                                                                                                                                                                              |
| scaling.downscale.limit                          | Maximum number of instances which can be removed with one downscaling decision                                                                                                                                                                                 |
| scaling.downscale.timeout                        | Time between two downscaling decisions (in seconds)                                                                                                                                                                                                            |
| scaling.safety.period                            | (Not implemented yet) Period for the safety scaler                                                                                                                                                                                                             |
| scaling.safety.ratio                             | (Not implemented yet) Ratio for the safety scaler                                                                                                                                                                                                              |
| scaling.kpis                                     | A map containing app names as keys and a list of KPIs as values. A KPI consists of a name (metric name as String), slo (upper bound as double) and query (optional, the Prometheus query to get this metric as String, if not set, name will be used as query) |
| training.interval                                | Time between two model trainings (in seconds). Note: You should also enable `training.exporter.enabled` so that new training data is available when retraining.                                                                                                |
| training.importer.import-metrics-from-database   | true if metrics for training should be queried from database, false to use CSV                                                                                                                                                                                 |
| training.importer.sources                        | Paths to look for training-data CSV files                                                                                                                                                                                                                      |
| training.exporter.enabled                        | true if metrics and autoscaler decisions should be exported to be used for training of improved models during runtime                                                                                                                                          |
| training.exporter.store-metrics-in-database      | true if training data should be written to database, false to use CSV export                                                                                                                                                                                   |
| training.exporter.metric-storage-exclusion-regex | Metrics which should not be included in the training data must match this regex                                                                                                                                                                                |
| training.training-lookback-days                  | Maximum age of data to retrieve for training in days. Use this to prevent out-of-memory errors when training.  Default: 32                                                                                                                                     |
| training.training-database-number-days-to-keep   | Maximum age of training data to keep in database in days. Older data is deleted periodically. Default: 93                                                                                                                                                      |

