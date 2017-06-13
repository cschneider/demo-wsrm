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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.cxf.Bus;
import org.apache.cxf.frontend.ClientProxy;
import org.apifocal.demo.greeter.Greeter;
import org.junit.After;
import org.junit.Assert;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ReliableInOrderTest extends KarafTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ReliableInOrderTest.class);
    private static final int COUNT = 50;

    @Inject
    @Filter("(cxf.bus.id=org.apifocal.demo.wsrm.greeter-wsrm-cxf*)")
    private Bus bus;

    private Greeter greeter;
    
    @Configuration
    public Option[] config() {
        MavenUrlReference demoFeaturesRepo = getFeaturesRepo("org.apifocal.demo.wsrm", "features");
        MavenUrlReference cxfFeaturesRepo = getFeaturesRepo("org.apache.cxf.karaf", "apache-cxf");

        return new Option[] {
            // debugConfig(),
            standardConfig(),
            cxfTestUtils(),
            features(cxfFeaturesRepo, "http", "cxf-http-jetty", "cxf-http-async"),
            // features(demoFeaturesRepo, "greeter-wsrm"),
            features(demoFeaturesRepo, "greeter-wsrm", "greeter-gateway"),
            editConfigurationFilePut("etc/org.apifocal.demo.gateway.qos.cfg", 
                "org.apifocal.demo.gateway.forward", "http://localhost:8181/cxf/greeter-once"),
            editConfigurationFilePut("etc/org.apifocal.demo.gateway.qos.cfg", 
                "org.apifocal.demo.gateway.policy", "delay"), // NOTE: possible values are "noop" or "delay"
       };
    }

    @Before
    public void createClient() {
        // NOTE: choices are: "greeter-wsrm", "greeter-once" or "greeter-exact"
        // greeter = OSGiTestHelper.greeterHttpProxy("8181", "greeter-once", "greeter-once");
        greeter = OSGiTestHelper.greeterHttpProxy("8181", "gateway-qos", "greeter-once");
    }

    @After
    public void destroyClient() {
        ClientProxy.getClient(greeter).destroy();
    }


    @Test
    public void testManyMessagesSync() throws Exception {
        LOG.info("Sync send test");

        // First message will trigger a CreateSequence
        await().ignoreExceptions().pollDelay(1, TimeUnit.SECONDS).until(() -> greeter.greetMe("World-0001"), is("Hello World-0001"));

        List<String> responses = new ArrayList<>();
        for (int i = 1; i < COUNT; i++) {
        	String who = String.format("World-%04d", i + 1);
        	Thread.sleep(20);
            responses.add(greeter.greetMe(who));
        }

        for (String r : responses) {
            LOG.info("Greeting: {}", r);
            Assert.assertTrue(r.startsWith("Hello World-0"));
        }
    }

    @Test
    public void testManyMessagesAsync() throws Exception {
        LOG.info("Async send test");

        // First message will trigger a CreateSequence
        await().ignoreExceptions().pollDelay(1, TimeUnit.SECONDS).until(() -> greeter.greetMe("World-0001"), is("Hello World-0001"));

        final AtomicInteger index = new AtomicInteger(1);
        List<Future<String>> responses = new ArrayList<Future<String>>(200);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 1; i < COUNT; i++) {
        	Thread.sleep(20);
            Future<String> future = executor.submit(() -> {
                String who = String.format("World-%04d", index.incrementAndGet());
                String result = "";
                synchronized(this) { greeter.greetMe(who); }
                return result;
            });
            responses.add(future);
        }

        int attempts = 64; // will give up after 20 attempts (about 2 min)
        while (responses.size() > 0 && --attempts > 0) {
            LOG.info("Giving the test a bit of time... still {} futures to handle", responses.size());
            Thread.sleep(5000);
        	for (Iterator<Future<String>> it = responses.iterator(); it.hasNext();) {
        		Future<String> f = it.next();
            	if (f.isDone()) {
            		try {
            			String r = f.get();
                        LOG.info("Greeting: {}", r);
                        Assert.assertTrue(r.startsWith("Hello World-0"));
            		} catch (Exception e) {
            			LOG.warn("Future execution resulted in exception {}: {}", e.getClass().getName(), e.getMessage());
            		}
                    it.remove();
            	}
            }
        }

        if (attempts <= 0) {
            Assert.fail("Gave up on wating for Future<> responses");
        }
    }

}
