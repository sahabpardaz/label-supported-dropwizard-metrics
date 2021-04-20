package ir.sahab.dropwizardmetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import java.util.Hashtable;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LabelSupportedObjectNameFactoryTest {

    private final static String METRICS_DOMAIN = "TestDomain";
    private static MBeanServer mBeanServer;

    private MetricRegistry metricRegistry;
    private JmxReporter jmxReporter;

    @Before
    public void before() {
        mBeanServer = MBeanServerFactory.createMBeanServer();
        metricRegistry = new MetricRegistry();
        jmxReporter = JmxReporter.forRegistry(metricRegistry)
                .createsObjectNamesWith(new LabelSupportedObjectNameFactory())
                .inDomain(METRICS_DOMAIN)
                .registerWith(mBeanServer)
                .build();
        jmxReporter.start();
    }

    @After
    public void after() {
        jmxReporter.close();
    }

    @Test
    public void testMetricWithoutLabel() throws MalformedObjectNameException {
        Counter counter = metricRegistry.counter(LabeledMetric.name("metricName").toString());
        counter.inc();

        Hashtable<String, String> keyProperties = new Hashtable<>();
        keyProperties.put("name", "metricName");
        assertTrue(mBeanServer.isRegistered(new ObjectName(METRICS_DOMAIN, keyProperties)));
    }

    @Test
    public void testMetricWithSingleLabel() throws MalformedObjectNameException {
        Counter counter = metricRegistry.counter(LabeledMetric.name("metricName")
                .label("label1", "value1").toString());
        counter.inc();

        Hashtable<String, String> keyProperties = new Hashtable<>();
        keyProperties.put("name", "metricName");
        keyProperties.put("label1", "value1");
        assertTrue(mBeanServer.isRegistered(new ObjectName(METRICS_DOMAIN, keyProperties)));
    }

    @Test
    public void testMetricWithMultipleLabels() throws MalformedObjectNameException {
        Counter counter = metricRegistry.counter(LabeledMetric.name("metricName")
                .label("label1", "value1").label("label2", "value2").label("label3", "value3")
                .toString());
        counter.inc();

        Hashtable<String, String> keyProperties = new Hashtable<>();
        keyProperties.put("name", "metricName");
        keyProperties.put("label1", "value1");
        keyProperties.put("label2", "value2");
        keyProperties.put("label3", "value3");
        assertTrue(mBeanServer.isRegistered(new ObjectName(METRICS_DOMAIN, keyProperties)));
    }

    @Test
    public void testMetricWithQuotedLabel() throws MalformedObjectNameException {
        Counter counter = metricRegistry.counter(LabeledMetric.name("metricName")
                .label("label1", "before?after").toString());
        counter.inc();

        Hashtable<String, String> keyProperties = new Hashtable<>();
        keyProperties.put("name", "metricName");
        keyProperties.put("label1", "\"before\\?after\"");
        assertTrue(mBeanServer.isRegistered(new ObjectName(METRICS_DOMAIN, keyProperties)));
    }

    @Test
    public void testMetricLabelsOrder() throws MalformedObjectNameException {
        Counter counter = metricRegistry.counter(LabeledMetric.name("metricName")
                .label("report", "reportName")
                .label("operator", "operatorName")
                .label("responsible", "responsibleName")
                .toString());
        counter.inc();

        Set<ObjectName> objectNames = mBeanServer.queryNames(new ObjectName(METRICS_DOMAIN + ":*"), null);
        assertEquals(1, objectNames.size());
        ObjectName registeredObjectName = objectNames.iterator().next();
        assertEquals("name=metricName,report=reportName,operator=operatorName,responsible=responsibleName",
                registeredObjectName.getKeyPropertyListString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMetricInvalidLabel1() {
        metricRegistry.counter(LabeledMetric.name("metricName")
                .label("name", "value").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMetricInvalidLabel2() {
        metricRegistry.counter(LabeledMetric.name("metricName")
                .label("type", "value").toString());
    }

    @Test(expected = AssertionError.class)
    public void testMetricInvalidLabel3() {
        metricRegistry.counter("metricName[key1]");
    }

    @Test(expected = AssertionError.class)
    public void testMetricInvalidLabel4() {
        metricRegistry.counter("metricName[key1=val1,key2]");
    }
}
