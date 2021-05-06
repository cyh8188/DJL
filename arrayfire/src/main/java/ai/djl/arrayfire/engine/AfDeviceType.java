/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.arrayfire.engine;

import ai.djl.Device;
import ai.djl.DeviceType;

/** DeviceType is the ArrayFire equivalent of the types in {@link Device}. */
public final class AfDeviceType implements DeviceType {

    private AfDeviceType() {}

    /**
     * Converts a {@link Device} to the corresponding ArrayFire device number.
     *
     * @param device the java {@link Device}
     * @return the ArrayFire device number
     */
    public static int toDeviceType(Device device) {
        String deviceType = device.getDeviceType();

        if (Device.Type.CPU.equals(deviceType)) {
            return 1;
        } else if (Device.Type.GPU.equals(deviceType)) {
            return 2;
        } else {
            throw new IllegalArgumentException("Unsupported device: " + device.toString());
        }
    }

    /**
     * Converts from an ArrayFire device number to {@link Device}.
     *
     * @param deviceType the ArrayFire device number
     * @return the corresponding {@link Device}
     */
    public static String fromDeviceType(int deviceType) {
        switch (deviceType) {
            case 1:
                return Device.Type.CPU;
            case 2:
                return Device.Type.GPU;
            default:
                throw new IllegalArgumentException("Unsupported deviceType: " + deviceType);
        }
    }
}
