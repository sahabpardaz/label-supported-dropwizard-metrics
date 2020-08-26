package ir.sahab.dropwizardmetrics;

import com.codahale.metrics.MetricRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A labeled metric name contains both the original metric name and its labels in this format:
 * metric-name[label1=value1,label2=value2].
 * You can create a labeled metric name easily by using this class.
 */
public class LabeledMetric {
    private String metricName;
    private Map<String, String> labels;

    private LabeledMetric(String metricName) {
        this.metricName = metricName;
        this.labels = new HashMap<>();
    }

    public static LabeledMetric name(String name, String... names) {
        return new LabeledMetric(MetricRegistry.name(name, names));
    }

    public LabeledMetric label(String labelName, String labelValue) {
        if (labelName.equals("name") || labelName.equals("type")) {
            throw new IllegalArgumentException("It is illegal to use label 'name' or 'type' for metric.");
        }
        labels.put(labelName, labelValue);
        return this;
    }

    public LabeledMetric labels(Map<String, String> labels) {
        for (Map.Entry<String, String> label : labels.entrySet()) {
            this.label(label.getKey(), label.getValue());
        }
        return this;
    }

    @Override
    public String toString() {
        if (labels.isEmpty()) {
            return metricName;
        }

        String labelsOfMetric = this.labels.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        return metricName + "[" + labelsOfMetric + "]";
    }
}
