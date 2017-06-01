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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apifocal.demo.greeter.Greeter;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public final class OSGiTestHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiTestHelper.class);

	private OSGiTestHelper() {
		// utility
	}

    public static Greeter greeterHttpProxy(String port, String address) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress("http://localhost:" + port + "/cxf/" + address);
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        factory.setFeatures(Collections.singletonList(loggingFeature));
        return factory.create(Greeter.class);
    }

    /**
     * Provides an iterable collection of references, even if the original array is null
     */
    public static Collection<ServiceReference<?>> asCollection(ServiceReference<?>[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference<?>>emptyList();
    }

    /*
     * Explode the dictionary into a ,-delimited list of key=value pairs
     */
    public static String explode(Dictionary<String, String> dictionary) {
        Enumeration<String> keys = dictionary.keys();
        StringBuilder result = new StringBuilder();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
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
