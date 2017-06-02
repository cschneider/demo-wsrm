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

import javax.inject.Inject;

import org.apache.cxf.Bus;
import org.apache.cxf.frontend.ClientProxy;
import org.apifocal.demo.greeter.Greeter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.util.concurrent.TimeUnit;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GreeterClientTest extends KarafTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GreeterClientTest.class);

    private Greeter greeter;
    
    @Inject
    @Filter("(cxf.bus.id=org.apifocal.demo.wsrm.greeter-wsrm-cxf*)")
    private Bus bus;

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
    public void createClient() {
        // TODO: add tests for "greeter-persist"
        greeter = OSGiTestHelper.greeterHttpProxy("8181", "greeter-wsrm");
    }


    @Test
    public void testCall() throws Exception {
        LOG.info("TEST started");
        await().ignoreExceptions().pollDelay(1, TimeUnit.SECONDS).until(() -> greeter.greetMe("World"), is("Hello World"));
        greeter.greetMe("World2");
        Thread.sleep(2000);
        greeter.greetMe("World23");
        Bundle bundle = bundle("org.apifocal.demo.wsrm.greeter-wsrm").get();
        bundle.stop();
        //Thread.sleep(5000);
        //greeter.greetMe("World24");
        bundle.start();
        Thread.sleep(2000);
        greeter.greetMe("World3");
        greeter.greetMe("World4");
        //await().ignoreExceptions().until(() -> greeter.greetMe("World3"), is("Hello World3"));
    }
    
    @After
    public void stop() {
        ClientProxy.getClient(greeter).destroy(); 
    }

}
