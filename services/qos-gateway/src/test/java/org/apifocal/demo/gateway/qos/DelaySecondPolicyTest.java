package org.apifocal.demo.gateway.qos;

import org.junit.Assert;
import org.junit.Test;

public class DelaySecondPolicyTest {
    @Test
    public void testGetNumber() {
        DelaySecondPolicy policy = new DelaySecondPolicy();
        String number = policy.getMessageNumber("wsrm:Identifier><wsrm:MessageNumber>50</wsrm:MessageNumber></wsrm:Sequence");
        Assert.assertEquals("50", number);
        
        String notFound = policy.getMessageNumber("wsrm:Identifier><wsrm:Message");
        Assert.assertNull(notFound);
    }
}
