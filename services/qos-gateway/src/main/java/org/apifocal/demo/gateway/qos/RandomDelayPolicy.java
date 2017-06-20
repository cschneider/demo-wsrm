/*
 * Copyright 2017 apifocal LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.demo.gateway.qos;

import java.io.IOException;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RandomDelayPolicy implements QosPolicy {
	private static final Logger LOG = LoggerFactory.getLogger(RandomDelayPolicy.class);
	private static final Random RAND = new Random();

	private static final int RATE_DELAY500 = 10;
	private static final int RATE_DELAY100 = 25;

	public void process(HttpServletRequest request) throws IOException {
        int  n = RAND.nextInt(100);

        int delay = 0;
        if (n < RATE_DELAY500) {
        	delay = 1000;
        } else if (n < RATE_DELAY500 + RATE_DELAY100) {
        	delay = 100;
        }

        try {
	    	LOG.info("Delaying by {}ms for message in the {}(%) percentile", delay, n);
			Thread.sleep(delay);
		} catch (InterruptedException e) { // ignore, it's unlikely, but ok
		}
	}

	@Override
	public String toString() {
		return "random-delay";
	}

}
