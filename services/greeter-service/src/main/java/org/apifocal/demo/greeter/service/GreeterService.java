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

import java.util.concurrent.Future;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apifocal.demo.greeter.Greeter;
import org.apifocal.demo.greeter.PingMeFault;
import org.apifocal.demo.greeter.types.GreetMeResponse;
import org.apifocal.demo.greeter.types.PingMeResponse;
import org.apifocal.demo.greeter.types.SayHiResponse;
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
        String response = "Hello " + name;
        LOG.info("Greeter salutes '{}' with '{}'", name, response);
        return response;
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

    @Override
    public Response<GreetMeResponse> greetMeAsync(String requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response<PingMeResponse> pingMeAsync() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<?> pingMeAsync(AsyncHandler<PingMeResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response<SayHiResponse> sayHiAsync() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

}
