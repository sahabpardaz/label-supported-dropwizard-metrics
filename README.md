# Label supported JMX metrics
Provides an object name factory for Dropwizard that adds the support for metric labels.
Labels are of great value for example on aggregating metrics by Prometheus.

### Sample Usage

The client code for referring to a label metric is like this:

```java
metricRegistry.counter(LabeledMetric.name("num_records").label("device_id", "1312").toString())
              .mark();
```

And if you do not have metric, you can call metric registry in its normal way:

```java
 metricRegistry.counter("num_records").mark();
```

But note that to make it work, you should also introduce the `LabelSupportedNameFactory` (provided by this library)
when starting the JMX reporter:

```java
JmxReporter.forRegistry(metricRegistry)
           .createsObjectNamesWith(new LabelSupportedObjectNameFactory())
           .inDomain("my-metrics-domain")
           .build().start();
```
