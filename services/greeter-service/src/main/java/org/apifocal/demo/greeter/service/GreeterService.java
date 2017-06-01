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
package org.apifocal.demo.greeter.service;

import javax.jws.WebService;

import org.apifocal.demo.greeter.Greeter;
import org.apifocal.demo.greeter.PingMeFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService(
    serviceName = "GreeterService",
    portName = "GreeterPort",
    endpointInterface = "org.apifocal.demo.greeter.Greeter")
public class GreeterService implements Greeter {
    private static final Logger LOG = LoggerFactory.getLogger(GreeterService.class);

    public GreeterService() {
        LOG.info("Greeter service starting...");
    }

    @Override
    public String greetMe(String name) {
        return "Hello " + name;
    }

    @Override
    public void pingMe() throws PingMeFault {
    }

    @Override
    public String sayHi() {
        return "Hi there...";
    }

    @Override
    public void greetMeOneWay(String name) {
    }

}
