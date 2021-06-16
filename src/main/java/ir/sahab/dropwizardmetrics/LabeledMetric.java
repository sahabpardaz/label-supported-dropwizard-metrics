package ir.sahab.dropwizardmetrics;

import com.codahale.metrics.MetricRegistry;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A labeled metric name contains both the original metric name and its labels in this format:
 * metric-name[label1=value1,label2=value2].
 * You can create a labeled metric name easily by using this class.
 */
public class LabeledMetric {
    private final StringBuilder nameAndLabels;
    private boolean hasLabel;

    private LabeledMetric(String labeledMetricName) {
        this.nameAndLabels = new StringBuilder(labeledMetricName);
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
     * Returns {@code true} when metric is labeled.
     */
    public static boolean hasLabel(String labeledMetricName) {
        return labeledMetricName.lastIndexOf(']') == labeledMetricName.length() - 1
                && labeledMetricName.indexOf('[') >= 1
                && labeledMetricName.indexOf('[') == labeledMetricName.lastIndexOf('[')
                && labeledMetricName.indexOf(']') == labeledMetricName.lastIndexOf(']');
    }

    /**
     * Extracts the name from a labeled metric name.
     */
    public static String extractMetricName(String labeledMetricName) {
        if (!hasLabel(labeledMetricName)) {
            return labeledMetricName;
        }
        return labeledMetricName.substring(0, labeledMetricName.indexOf("["));
    }

    /**
     * Takes a metric string and applies the consumer on each one of it's labels. Consumer takes a string array of
     * length 2 which first one is label name and second one is label value.
     */
    public static void processLabels(String labeledMetricName, Consumer<String[]> processor) {
        if (!hasLabel(labeledMetricName)) {
            return;
        }

        String labelString = labeledMetricName.substring(labeledMetricName.indexOf('[') + 1,
                labeledMetricName.lastIndexOf(']'));

        for (String label : labelString.split(",")) {
            final String[] keyValue = label.split("=");
            if (keyValue.length == 2) {
                processor.accept(keyValue);
            } else {
                throw new AssertionError("Invalid metric provided: " + labeledMetricName);
            }
        }
    }


    @Override
    public String toString() {
        if (hasLabel) {
            nameAndLabels.append(']');
        }
        return nameAndLabels.toString();
    }
}
