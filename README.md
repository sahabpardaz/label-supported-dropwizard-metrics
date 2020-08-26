# Label supported JMX metrics
Provides an object name factory for Dropwizard that adds the support for metric labels.
Labels are of great value for example on aggregating metrics by Prometheus.

### Sample Usage

```java
public static void main(String[] args) throws Exception {
    MetricRegistry metricRegistry = new MetricRegistry();
    JmxReporter.forRegistry(metricRegistry)
            .createsObjectNamesWith(new LabelSupportedObjectNameFactory())
            .inDomain("my-metrics-domain")
            .build().start();

    metricRegistry.counter(
        LabeledMetric.name("num_records").label("device_id", "123").toString())
        .mark();
}
```
