/**
 * Copyright (C) 2017 apifocal LLC - https://www.apifocal.com
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apifocal.demo.wsrm.itests;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public abstract class KarafTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KarafTestSupport.class);
    private static final String DISTRO_GID = "org.apache.karaf";
    private static final String DISTRO_AID = "apache-karaf";
    private static final Long SERVICE_TIMEOUT = 4000L;

    @Inject
    protected BootFinished bootFinished;

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featureService;

    protected MavenUrlReference karafUrl;
    protected ExecutorService executor = Executors.newCachedThreadPool();

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        // probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    /**
     * Create an {@link org.ops4j.pax.exam.Option} for TODO: document further!
     *
     * @return
     */
    protected Option standardConfig() {
        karafUrl = maven().groupId(DISTRO_GID).artifactId(DISTRO_AID).version(getKarafVersion()).type("tar.gz");
        MavenUrlReference karafStandardRepo = getFeaturesRepo("org.apache.karaf.features", "standard");
        // MavenUrlReference cxfFeaturesRepo = getFeaturesRepo("org.apache.cxf.karaf", "apache-cxf");
        // MavenUrlReference amqFeaturesRepo = getFeaturesRepo("org.apache.activemq", "activemq-karaf");

        String localRepo = System.getProperty("localRepository");
        Object urp = System.getProperty("demo.useRandomFirstPort");
        return composite(
            karafDistributionConfiguration()
                .frameworkUrl(karafUrl)
                .karafVersion(getKarafVersion())
                .name("Apache Karaf")
                .useDeployFolder(false)
                .unpackDirectory(new File("target/paxexam/")),
            // systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
            systemProperty("java.awt.headless").value("true"),
	        configureConsole().ignoreLocalConsole(),
            logLevel(LogLevel.WARN),
            replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File("src/test/resources/etc/org.ops4j.pax.logging.cfg")),
	        // features(karafStandardRepo, "scr"),
            mavenBundle("org.awaitility", "awaitility").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
            when(localRepo != null).useOptions(
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", localRepo)),
            when(urp != null).useOptions(systemProperty("cxf.useRandomFirstPort").value("true")));
    }

    protected Option demoDebugConfig() {
        return composite(standardConfig(),
            debugConfiguration(),
            keepRuntimeFolder(),
            logLevel(LogLevel.DEBUG)
        );
    }

    protected Option cxfTestUtils() {
        return mavenBundle().groupId("org.apache.cxf").artifactId("cxf-testutils").versionAsInProject();
    }

    protected void assertBundleStarted(String name) {
        Bundle bundle = findBundleByName(name);
        Assert.assertNotNull("Bundle " + name + " should be installed", bundle);
        Assert.assertEquals("Bundle " + name + " should be started", Bundle.ACTIVE, bundle.getState());
    }

    protected Bundle findBundleByName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    protected void assertServicePublished(String filter, int timeout) {
        try {
            Filter serviceFilter = bundleContext.createFilter(filter);
            ServiceTracker<Object, ?> tracker = new ServiceTracker<>(bundleContext, serviceFilter, null);
            tracker.open();
            Object service = tracker.waitForService(timeout);
            tracker.close();
            if (service == null) {
                throw new IllegalStateException("Expected service with filter " + filter + " was not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception occured", e);
        }
    }

    protected void assertBlueprintNamespacePublished(String namespace, int timeout) {
        assertServicePublished(String.format(
            "(&(objectClass=org.apache.aries.blueprint.NamespaceHandler)(osgi.service.blueprint.namespace=%s))", namespace), timeout);
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter) {
        return getOsgiService(type, filter, SERVICE_TIMEOUT);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String f;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    f = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    f = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                f = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(f);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);

            // TODO: Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                Dictionary<String, String> dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + OSGiTestHelper.explode(dic));

                for (ServiceReference<?> ref : OSGiTestHelper.asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref);
                }

                for (ServiceReference<?> ref : OSGiTestHelper.asCollection(bundleContext.getAllServiceReferences(null, f))) {
                    System.err.println("Filtered ServiceReference: " + ref);
                }

                throw new RuntimeException("Gave up waiting for service " + f);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds a free port starting from the give port number.
     *
     * @return
     */
    protected int getFreePort(int port) {
        while (!OSGiTestHelper.isPortAvailable(port)) {
            port++;
        }
        return port;
    }

    public static String getKarafVersion() {
        return MavenUtils.getArtifactVersion(DISTRO_GID, DISTRO_AID);
    }

    public static MavenUrlReference getFeaturesRepo(String gid, String aid) {
    	return getFeaturesRepo(gid, aid, null);
    }

    public static MavenUrlReference getFeaturesRepo(String gid, String aid, String version) {
    	MavenUrlReference ref = maven().groupId(gid).artifactId(aid).type("xml").classifier("features");
    	return version == null ? ref.versionAsInProject() : ref.version(version);
    }

}
