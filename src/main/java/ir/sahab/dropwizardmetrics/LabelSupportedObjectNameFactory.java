package ir.sahab.dropwizardmetrics;

import com.codahale.metrics.jmx.ObjectNameFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * It is a custom implementation of {@code ObjectNameFactory} which supports exposing metric labels in JMX.
 * Labels are of great value for example on aggregating metrics by Prometheus.
 *
 * <p>In fact a JMX {@link ObjectName} contains a domain and a list of key values. The default implementation
 * of {@link ObjectNameFactory} sets the domain and puts two other key values, one for the name of the metric
 * and one for its type. Here we do the same, but we also put user provided labels in the key value list too.
 * But the trick is how to get labels? Dropwizard methods for defining metrics do not support labels. They just get
 * a string for metric name. So we are going to incorporate the labels in that string. We call it labeled metric name.
 * A labeled metric name contains both the original metric name and its labels in this format:
 * metric-name[label1=value1,label2=value2].
 * You can make and use a labeled metric name easily by using {@link LabeledMetric}.
 *
 * <p>So the client code for referring to a label metric is like this:
 * <pre>
 *  metricRegistry.counter(LabeledMetric.name("num_records").label("device_id", "1312").toString()).mark();
 * </pre>
 * It is possible to have metrics without labels same as before.
 *
 * <p>Although to make it work, you should also introduce this name factory when starting the JMX reporter:
 * <pre>
 *  JmxReporter.forRegistry(metricRegistry)
 *             .createsObjectNamesWith(new LabelSupportedObjectNameFactory())
 *             .inDomain("my-metrics-domain")
 *             .build().start();
 * </pre>
 *
 * <p>Note: Different versions of {code io.dropwizard.metrics}, has different implementation for
 *       {@code DefaultObjectNameFactory}. For compatibility with your previous metrics, check it before using.</p>
 */
public class LabelSupportedObjectNameFactory implements ObjectNameFactory {

    /**
     * Converts a metric to a JMX {@link ObjectName}.
     *
     * <p>Note: Dropwizard misuse {@code IllegalArgumentException} for checking duplicate metric name, so we use
     * {@code AssertionError} for invalid metric names.
     * @return JMX {@link ObjectName} corresponding to that metric.
     */
    @Override
    public ObjectName createName(String type, String domain, String labeledMetricName) {
        String metricName = LabeledMetric.extractMetricName(labeledMetricName);
        String quotedLabels = extractQuotedLabelValues(labeledMetricName);

        String name = quoteDomainIfRequired(domain) + ":name=" + quoteValueIfRequired(metricName) + quotedLabels;
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException finalException) {
            throw new AssertionError(finalException);
        }
    }

    /**
     * Quotes domain of {@link ObjectName} if it is required.
     */
    private static String quoteDomainIfRequired(String domain) {
        // Based on {@code ObjectName} implementation, The only way we can find out if we need to quote the domain
        // is by checking an {@code ObjectName} that we've constructed.
        ObjectName objectName;
        try {
            // Key properties cannot be empty, so we provide dummy key value.
            objectName = new ObjectName(domain, "key", "value");
            if (objectName.isDomainPattern()) {
                domain = ObjectName.quote(domain);
            }
            objectName = new ObjectName(domain, "key", "value");
        } catch (MalformedObjectNameException e) {
            try {
                domain = ObjectName.quote(domain);
                objectName = new ObjectName(domain, "key", "value");
            } catch (MalformedObjectNameException finalException) {
                throw new AssertionError("Invalid domain: " + domain, finalException);
            }
        }
        return domain;
    }

    /**
     * Quotes value of {@link ObjectName} property if it is required.
     */
    private static String quoteValueIfRequired(String propertyValue) {
        // Based on {@code ObjectName} implementation, The only way we can find out if we need to quote the properties
        // is by checking an {@code ObjectName} that we've constructed.
        ObjectName objectName;
        try {
            objectName = new ObjectName("domain", "key", propertyValue);
            if (objectName.isPropertyValuePattern("key")) {
                propertyValue = ObjectName.quote(propertyValue);
            }
            objectName = new ObjectName("domain", "key", propertyValue);
        } catch (MalformedObjectNameException e) {
            try {
                propertyValue = ObjectName.quote(propertyValue);
                objectName = new ObjectName("domain", "key", propertyValue);
            } catch (MalformedObjectNameException finalException) {
                throw new AssertionError("Invalid property value: " + propertyValue, finalException);
            }
        }
        return propertyValue;
    }

    /**
     * Extracts and quotes values of label if required. If metric has labels, the returned value
     * has a leading ',' for easier concatenation to other parts of ObjectName.
     *
     * Note: Spaces are significant everywhere in an Object Name. Do not
     * write "metric_name[type=Thread, name=DGC] (with a space after the comma) because it will be interpreted as having
     * a key called " name", with a leading space in the name.
     */
    private static String extractQuotedLabelValues(String labeledMetricName) {
        if (!LabeledMetric.hasLabel(labeledMetricName)) {
            return "";
        }

        StringBuilder labelBuilder = new StringBuilder(labeledMetricName.length() + 1);
        LabeledMetric.processLabels(labeledMetricName, labelAndValue -> labelBuilder.append(',')
                        .append(labelAndValue[0])
                        .append('=')
                        .append(quoteValueIfRequired(labelAndValue[1])));
        return labelBuilder.toString();
    }
}
