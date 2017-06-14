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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelaySecondPolicy implements QosPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(DelaySecondPolicy.class);

    public void process(HttpEntityEnclosingRequest proxyRequest) throws IOException {
        HttpEntity entity = proxyRequest.getEntity();
        String content = MorpheusHelper.readStream(entity.getContent());
        System.out.println(content);
        String number = getMessageNumber(content);
        if ("2".equals(number)) {
            LOG.info("Delaying message " + number);
            delay(1000);
        }
    }

    String getMessageNumber(String content) {
        Pattern pattern = Pattern.compile(".*<wsrm:MessageNumber>(.*)</wsrm:MessageNumber>.*");
        Matcher matcher = pattern.matcher(content);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private void delay(long delay) {
        try {
            LOG.info("Delaying {}ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // ignore, it's unlikely, but ok
        }
    }

    @Override
    public String toString() {
        return "delaySecond";
    }

}
