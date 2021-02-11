# Nuxeo Statistics

This addon allows precomputing statistics with custom computers, which will then be stored in a key-value store.

Statistics are also made available as metrics with no added cost.
For efficiency reasons, the computed metric values are cached in the KV Store so that metrics can be retrieved for free.

<table>
<tr><td>
<img src="doc/usage-grafana.png" width="400px"/>
</td>
<td>
<img src="doc/nx-stats.png" width="400px"/>
</td></tr>
<tr><td>
 App usage in System monitoring
</td>
<td>
 App usage in Application Dashboard
</td></tr>
</table> 

## Principles

### Computing metrics asynchronously

The idea is to have asynchronous tasks running periodically to compute some statistics.

Technically, the Nuxeo-Stream computation system is used to:

 - elect one of the nodes in the Nuxeo Cluster to report the metrics
 - start the processing periodically

### Exposed as a cached metric

The result of the computation is stored in the KV Store and exposed as a Metrics.

### Metrics framework

The fact that the statistics are exposed via metrics provide a few key advantages:

 - you can use the reporter of your choice
 - you can easily add a TSDB to store the evolution on the time dimension
 - you can leverage existing tools for graphing

### Leverage Stream for metrics history retention

While some dashboards can be built using native TSDB and tools like Grafana/Graphite, we also want to allow projects to build Application level dashboard without having to bridge the "Ops Dashboard" system with the application layer (Network accessibility, security ...).

The idea is to periodically collect a set of metrics (the statistic metrics but also potentially others) and archive them as a record in a Nuxeo stream.
This way, the target Nuxeo Stream becomes a history of the metrics snapshots, and the underlying Kafka topic retention allows to control how long we want to keep history.

### Application-level Aggregates and API

Using a Computation, we can read the whole history available in Nuxeo Stream, compute an aggregate and store the result in the KVStore.
Using a simple Automation Operation, we can retrieve the full time-series for all metrics and plot graphs on the client-side.

### Logical Architecture

<img src="doc/Metrics-and-API-principles.png" width="800px">

## Current Exposed Statistics

### Document Counts

The Metric name is `nuxeo.statistics.repository.documents`.
The tags added to the Metric are:
    - `repository` for the repository name
    - `doctype` for the document type
The value is the number of documents for the given document type in the target repository.

Typically, on a repository called default and having only 3 documents we would get:

    nuxeo.statistics.repository.documents{doctype=File, repository=default}:2
    nuxeo.statistics.repository.documents{doctype=Folder, repository=default}:1

### Blobs size 

The total size of the main blobs is computed and exposed via a metric.

The Metric name is `nuxeo.statistics.repository.blobs.mainBlobs`.
The tags added to the Metric are:
    - `repository` for the repository name

The value is the total volume in bytes for the main blobs attached to all documents in the repository.

For example:

    nuxeo.statistics.repository.blobs.mainBlobs{repository=default}:15000

### Events statistics

The system computes an aggregate for all events in the Audit Log for the last hour.

The Metric name is `nuxeo.statistics.audit.events`.
The tags added to the Metric are:
    - `event` for the name of the event

If during the last hour 3 documents were created and 1 modified:   

    nuxeo.statistics.audit.events{event=documentCreated}:3
    nuxeo.statistics.audit.events{event=documentModified}:1

### Active Users statistics

The number of active users is computed by extracting unique users associated with events in the Audit log over the last hour.

The assumption is that if a user was active during the last hour, we should find in Audit at least one of the following events:

login or logout
download a file
search
create / update

The Metric name is `nuxeo.statistics.active.users`.

There are no tags added to this Metric.

## API

Statistics are exposed using an Automation Operation called `Statistics.Fetch`.

This operation supports 2 types of call:

### Void input : retrieve metrics with timeseries

 - operation: `Statistics.Fetch`
 - input: void
 - parameters: 
    - `filter` : Regular Expression applied to metric name
    - `start` : start time to extract metric from
        - all metrics returned will have a timestamp <= start
        - start is a timestamp expressed in seconds (i.e. `System.currentTimeMillis()/1000`)
    - `duration` : range of the time serie
        - duration will be parsed as a [Java Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-)
        - Operation will automatically add the `PT` prefix if needed
            - "5s" will be understood as 5 seconds
            - "5d" will be understood as 5 days
        - if `start` is not set it will be initialized to `System.currentTimeMillis()/1000`
        - all metrics returned will have a start-duration <= timestamp <= start
        
 - return: JSON [{metric1_ts1, metric2_ts1 ... ts1} {metric1_ts2, metric2_ts2 ... ts2} ... ]

    [
        {
            "nuxeo.statistics.audit.events.documentCreated":7,
            "nuxeo.statistics.audit.events.documentModified":1,
            "nuxeo.statistics.repository.documents.Domain.test":1,
            "nuxeo.statistics.audit.events.loginSuccess":1,
            "nuxeo.statistics.audit.events.documentSecurityUpdated":2,
            "nuxeo.statistics.repository.blobs.mainBlobs.test":15,
            "nuxeo.statistics.repository.documents.Folder.test":1,
            "nuxeo.statistics.repository.documents.File.test":2,
            "ts":1612897866
        },
        {
            "nuxeo.statistics.audit.events.documentCreated":7,
            "nuxeo.statistics.audit.events.documentModified":1,
            "nuxeo.statistics.repository.documents.Domain.test":1,
            "nuxeo.statistics.audit.events.loginSuccess":1,
            "nuxeo.statistics.audit.events.documentSecurityUpdated":2,
            "nuxeo.statistics.repository.blobs.mainBlobs.test":15,
            "nuxeo.statistics.repository.documents.Folder.test":1,
            "nuxeo.statistics.repository.documents.File.test":2,
            "ts":1612897876
        }
    ]

### String input : retrieve the current value for one metric

 - operation: `Statistics.Fetch`
 - input: metric name
 - parameter: none
 - return: Long (value of the metric)

Because metric names also have dimensions (i.e. repository, event name, doctype ...), you need to build a metric name that also includes the dimension you want.

Typically, for `nuxeo.statistics.repository.blobs.mainBlobs{repository=default}`, the target name to use as input is:

    nuxeo.statistics.repository.blobs.mainBlobs.default

For `nuxeo.statistics.repository.documents{doctype=File, repository=default}` 
    
    nuxeo.statistics.repository.documents.File.test

Values for the tags are appended to the metric name using the alphabetical order of the tagNames.

## Configuration

### Understanding the different layers

The statistics are computed using several periodic tasks:

**Step 1 - StatisticsComputer - compute the metric:**

`StatisticsComputer`s are contributed to `StatisticsService` and are in charge of computing some metrics.
Typically:

 - `ESAuditStatisticsComputer`: compute metrics based on Audit log in Elasticsearch
 - `ESRepositoryStatisticsComputer`: computes metrics related to the Document Repository using Elasticsearch index

 Each `StatisticsComputer` defines its refresh interval and is run by its own `StatisticsComputation`.

**Step 2 - MetricsHistoryCollector - Capture Snapshots of metrics**

`StreamMetricsHistoryCollector` is deployed via a `MetricsReporter` and is in charge of capturing some metrics and save their value in the nuxeo stream `statistics/history`.

At this level, you can configure:

 - metrics filters
 - periodicity

**Step 3 - StatisticTSAggregate - Compite Timeseries aggregations**

`StatisticTSAggregateComputation` is a scheduled computation that is in charge of aggregating all metrics available in `statistics/history`.

At this level, you can configure:

 - periodicity of aggregations
 - retention is managed directly at the nuxeo-stream/Kafka level

### Configure StatisticsComputer

`StatisticsComputer`s are directly contributed to the `StatisticsService`

    <extension point="computers" target="org.nuxeo.statistics.service">
        <computer name="repository" 
            interval="5m" 
            class="org.nuxeo.statistics.repository.impl.ESRepositoryStatisticsComputer" />
    </extension>

If unset, the interval will be taken from `nuxeo.conf` using the property

    nuxeo.statistics.computer.default.interval=1h

NB: 1h is the default value.

### Configure MetricsHistoryCollector

The metric history collector will run periodically using interval from `nuxeo.conf`

    nuxeo.statistics.snapshot.default.interval=5m

NB: 5m is the default value.

XXX Filter is not yet configurable.

### Configure TSAggregate

Aggregate scheduled computation can be configured using `nuxeo.conf`

    nuxeo.statistics.aggregate.default.interval=5m

NB: 5m is the default value.

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).