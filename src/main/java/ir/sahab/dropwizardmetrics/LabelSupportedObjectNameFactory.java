package ir.sahab.dropwizardmetrics;

import com.codahale.metrics.jmx.ObjectNameFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
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

        StringBuilder nameBuilder = new StringBuilder(domain.length() + labeledMetricName.length() + 5);
        nameBuilder.append(quoteDomainIfRequired(domain)).append(':');
        String metricName = extractMetricName(labeledMetricName);
        nameBuilder.append("name=").append(quoteValueIfRequired(metricName));

        Map<String, String> labels = extractLabels(labeledMetricName);
        for (Entry<String, String> label : labels.entrySet()) {
            nameBuilder.append(',');
            nameBuilder.append(label.getKey()).append('=').append(quoteValueIfRequired(label.getValue()));
        }

        try {
            return new ObjectName(nameBuilder.toString());
        } catch (MalformedObjectNameException finalException) {
            throw new AssertionError(finalException);
        }
    }

    /**
     * Quotes domain of {@link ObjectName} if it is required.
     */
    private String quoteDomainIfRequired(String domain) {
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
    private String quoteValueIfRequired(String propertyValue) {
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
     * Returns {@code true} when metric name is a labeled metric name.
     */
    private boolean hasLabel(String name) {
        return name.lastIndexOf(']') == name.length() - 1
                && name.indexOf('[') >= 1
                && name.indexOf('[') == name.lastIndexOf('[')
                && name.indexOf(']') == name.lastIndexOf(']');
    }

    /**
     * Extracts the name from a labeled metric name.
     */
    private String extractMetricName(String labeledMetricName) {
        if (!hasLabel(labeledMetricName)) {
            return labeledMetricName;
        }
        return labeledMetricName.substring(0, labeledMetricName.indexOf("["));
    }

    /**
     * Extracts the labels from a labeled metric name. Note: Spaces are significant everywhere in an Object Name. Do not
     * write "metric_name[type=Thread, name=DGC] (with a space after the comma) because it will be interpreted as having
     * a key called " name", with a leading space in the name.
     */
    private Map<String, String> extractLabels(String labeledMetricName) {
        if (!hasLabel(labeledMetricName)) {
            return Collections.emptyMap();
        }

        String labelsList = labeledMetricName.substring(
                labeledMetricName.indexOf("[") + 1, labeledMetricName.lastIndexOf("]"));
        Map<String, String> labels = new LinkedHashMap<>();
        for (String label : labelsList.split(",")) {
            String[] labelParts = label.split("=");
            if (labelParts.length != 2) {
                throw new AssertionError("Illegal label provided: " + label);
            }
            labels.put(labelParts[0], labelParts[1]);
        }

        return labels;
    }
}
