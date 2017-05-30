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

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;


/**
 *
 */
public abstract class KarafTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KarafTestSupport.class);
    private static final String DISTRO_GID = "org.apache.karaf";
    private static final String DISTRO_AID = "apache-karaf-minimal";

    @Inject
    protected BootFinished bootFinished;

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featureService;

    protected MavenUrlReference karafUrl;
    protected ExecutorService executor = Executors.newCachedThreadPool();

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
            systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
            systemProperty("java.awt.headless").value("true"),
	        configureConsole().ignoreLocalConsole(),
            logLevel(LogLevel.INFO),
            replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File("src/test/resources/etc/org.ops4j.pax.logging.cfg")),
	        features(karafStandardRepo, "scr"),
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

    public void assertServicePublished(String filter, int timeout) {
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

    public void assertBlueprintNamespacePublished(String namespace, int timeout) {
        assertServicePublished(String.format(
            "(&(objectClass=org.apache.aries.blueprint.NamespaceHandler)(osgi.service.blueprint.namespace=%s))", namespace), timeout);
    }

    public static String getKarafVersion() {
        return MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf-minimal");
    }

    public static MavenUrlReference getFeaturesRepo(String gid, String aid) {
    	return getFeaturesRepo(gid, aid, null);
    }

    public static MavenUrlReference getFeaturesRepo(String gid, String aid, String version) {
    	MavenUrlReference ref = maven().groupId(gid).artifactId(aid).type("xml").classifier("features");
    	return version == null ? ref.versionAsInProject() : ref.version(version);
    }

    /**
     * Finds a free port starting from the give port number.
     *
     * @return
     */
    protected int getFreePort(int port) {
        while (!isPortAvailable(port)) {
            port++;
        }
        return port;
    }

    /**
     * Returns true if port is available for use.
     * TODO: check if functionality is available from dependencies.
     *
     * @param port
     * @return
     */
    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        try (DatagramSocket ds = new DatagramSocket(port)) {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            // ignore
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

}
