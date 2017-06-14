package org.apifocal.demo.gateway.qos;

public class PolicyFactory {

    static QosPolicy create(String policyName) {
        if ("randomDelay".equals(policyName)) {
            return new RandomDelayPolicy();
        } else if ("delaySecond".equals(policyName)) {
            return new DelaySecondPolicy();
        } else {
            return new NoopPolicy();
        }
    }
}
