/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package ai.djl;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DeviceTest {

    @Test
    public void testDevice() {
        Assert.assertEquals(Device.cpu(), Device.of("cpu", -1));

        if (Device.getGpuCount() > 0) {
            Assert.assertEquals(Device.gpu(), Device.defaultDevice());
            Assert.assertEquals(Device.gpu(), Device.of("gpu", 0));
        } else {
            Assert.assertEquals(Device.cpu(), Device.defaultDevice());
            Assert.assertNull(Device.gpu());
        }

        if (Device.getGpuCount() > 2) {
            Assert.assertEquals(Device.gpu(3), Device.of("gpu", 3));
        } else {
            Assert.assertNull(Device.gpu(3));
        }

        Assert.assertNotEquals(Device.cpu(), Device.gpu());
        Device dev = Device.of("myDevice", 1);
        Assert.assertNotNull(dev);
        Assert.assertEquals(dev.getDeviceType(), "myDevice");
    }
}
