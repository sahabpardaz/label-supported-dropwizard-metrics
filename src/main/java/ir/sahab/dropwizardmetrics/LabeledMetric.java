package ir.sahab.dropwizardmetrics;

import com.codahale.metrics.MetricRegistry;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A labeled metric name contains both the original metric name and its labels in this format:
 * metric-name[label1=value1,label2=value2].
 * You can create a labeled metric name easily by using this class.
 */
public class LabeledMetric {
    private final StringBuilder nameAndLabels;
    private boolean hasLabel;

    private LabeledMetric(String metricName) {
        this.nameAndLabels = new StringBuilder(quoteValueIfRequired(metricName));
        this.hasLabel = false;
    }

    public static LabeledMetric name(String name, String... names) {
        return new LabeledMetric(MetricRegistry.name(name, names));
    }

    public LabeledMetric label(String labelName, String labelValue) {
        if (labelName.equals("name") || labelName.equals("type")) {
            throw new IllegalArgumentException("It is illegal to use label 'name' or 'type' for metric.");
        }

        if (hasLabel) {
            nameAndLabels.append(',');
        } else {
            nameAndLabels.append('[');
        }
        this.hasLabel = true;
        nameAndLabels.append(labelName).append('=').append(quoteValueIfRequired(labelValue));
        return this;
    }

    public LabeledMetric labels(Map<String, String> labels) {
        for (Map.Entry<String, String> label : labels.entrySet()) {
            this.label(label.getKey(), label.getValue());
        }
        return this;
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
                objectName = new ObjectName("domain", "key", propertyValue);
            }
        } catch (MalformedObjectNameException e) {
            try {
                propertyValue = ObjectName.quote(propertyValue);
                objectName = new ObjectName("domain", "key", propertyValue);
            } catch (MalformedObjectNameException finalException) {
                throw new IllegalArgumentException("Invalid property value: " + propertyValue, finalException);
            }
        }
        return propertyValue;
    }

    @Override
    public String toString() {
        if (hasLabel) {
            nameAndLabels.append(']');
        }
        return nameAndLabels.toString();
    }
}
