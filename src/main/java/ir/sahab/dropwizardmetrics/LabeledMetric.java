package ir.sahab.dropwizardmetrics;

import com.codahale.metrics.MetricRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A labeled metric name contains both the original metric name and its labels in this format:
 * metric-name[label1=value1,label2=value2].
 * You can create a labeled metric name easily by using this class.
 */
public class LabeledMetric {
    private final StringBuilder nameAndLabels;
    private boolean hasLabel;

    private LabeledMetric(String metricName) {
        this.nameAndLabels = new StringBuilder(metricName);
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
        nameAndLabels.append(labelName).append('=').append(labelValue);
        return this;
    }

    public LabeledMetric labels(Map<String, String> labels) {
        for (Map.Entry<String, String> label : labels.entrySet()) {
            this.label(label.getKey(), label.getValue());
        }
        return this;
    }

    /**
     * Returns {@code true} when metric name is a labeled metric name.
     */
    public static boolean hasLabel(String name) {
        return name.lastIndexOf(']') == name.length() - 1
                && name.indexOf('[') >= 1
                && name.indexOf('[') == name.lastIndexOf('[')
                && name.indexOf(']') == name.lastIndexOf(']');
    }

    /**
     * Returns map of label name to label value. If metric is not a labeled one, it returns empty map.
     */
    public static Map<String, String> extractLabels(String name) {
        if (!hasLabel(name)) {
            return Collections.emptyMap();
        }

        String labelString = name.substring(name.indexOf('[') + 1, name.lastIndexOf(']'));

        final Map<String, String> labels = new LinkedHashMap<>();
        for (String label : labelString.split(",")) {
            final String[] keyValue = label.split("=");
            if (keyValue.length == 2) {
                labels.put(keyValue[0], keyValue[1]);
            } else {
                throw new AssertionError("Invalid metric name provided: " + name);
            }
        }
        return labels;
    }


    @Override
    public String toString() {
        if (hasLabel) {
            nameAndLabels.append(']');
        }
        return nameAndLabels.toString();
    }
}
