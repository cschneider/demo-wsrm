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
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class KarafConfigurationTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
		MavenUrlReference demoFeaturesRepo = getFeaturesRepo("org.apifocal.demo.wsrm", "features");

	    return new Option[] {
	        standardConfig(),
	        features(demoFeaturesRepo, "greeter-wsrm"),
	   };
	}

	@Test
    public void testGreeterClient() throws Exception {
        Assert.assertNotNull(bootFinished);

        assertBundleStarted("org.apache.karaf.bundle.core");

        // Bundle-SymbolicName = org.apifocal.demo.wsrm.greeter-wsrm
        assertBundleStarted("org.apifocal.demo.wsrm.greeter-wsrm");
    }

}
