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

import org.apache.http.HttpEntityEnclosingRequest;

/**
 * Drops no messages.
 */
public class NoopPolicy implements QosPolicy {

	public void process(HttpEntityEnclosingRequest proxyRequest) throws IOException {
		// noop, do nothing
	}

	@Override
	public String toString() {
		return "noop";
	}

}
