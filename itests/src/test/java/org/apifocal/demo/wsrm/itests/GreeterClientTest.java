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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apifocal.demo.greeter.Greeter;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GreeterClientTest extends KarafTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GreeterClientTest.class);

    private Greeter greeter;

	@Configuration
	public Option[] config() {
		MavenUrlReference demoFeaturesRepo = getFeaturesRepo("org.apifocal.demo.wsrm", "features");
        MavenUrlReference cxfFeaturesRepo = getFeaturesRepo("org.apache.cxf.karaf", "apache-cxf");

	    return new Option[] {
	        standardConfig(),
	        cxfTestUtils(),
	        features(cxfFeaturesRepo, "http", "cxf-http-jetty"),
	        features(demoFeaturesRepo, "greeter-wsrm"),
	   };
	}

    @Before
    public void enablePrettyLogging() throws Exception {
    	prettyLogging();
    }

    @Before
    public void createClient() {
        greeter = OSGiTestHelper.greeterHttpProxy("8181", "greeter-wsrm");
    }


	@Test
    public void testNoop() throws Exception {
    }

    @Test
    public void testContainerConfiguration() throws Exception {
        LOG.info("TEST started");
        Assert.assertNotNull(bootFinished);
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getState() != 32)
            System.out.println(b.getSymbolicName() + " " + b.getState());
        }
        // assertBundleStarted("org.apifocal.demo.wsrm.greeter-wsrm");
        assertServicePublished("(&(objectClass=org.apache.cxf.Bus)(cxf.bus.id=org.apifocal.demo.wsrm.greeter-wsrm-cxf*))", 2000);

        String result = greeter.greetMe("World");
        Assert.assertEquals("Hello World", result);
    }

}
