/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.gluonTS.timeFeature;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.util.PairList;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/** this is a class to generate time feature by frequency. */
public class TimeFeature {

    private static final PairList<String, List<BiFunction<NDManager, List<LocalDateTime>, NDArray>>>
            FEATURES_BY_OFFSETS =
                    new PairList<String, List<BiFunction<NDManager, List<LocalDateTime>, NDArray>>>(
                            12) {
                        {
                            add("Y", Collections.emptyList());
                            add("Q", Arrays.asList(TimeFeature::monthOfYear));
                            add("M", Arrays.asList(TimeFeature::monthOfYear));
                            add(
                                    "W",
                                    Arrays.asList(
                                            TimeFeature::dayOfMonth, TimeFeature::weekOfYear));
                            add(
                                    "D",
                                    Arrays.asList(
                                            TimeFeature::dayOfWeek,
                                            TimeFeature::dayOfMonth,
                                            TimeFeature::dayOfYear));
                            add(
                                    "H",
                                    Arrays.asList(
                                            TimeFeature::hourOfDay,
                                            TimeFeature::dayOfWeek,
                                            TimeFeature::dayOfMonth,
                                            TimeFeature::dayOfYear));
                            add(
                                    "T",
                                    Arrays.asList(
                                            TimeFeature::minuteOfHour,
                                            TimeFeature::hourOfDay,
                                            TimeFeature::dayOfWeek,
                                            TimeFeature::dayOfMonth,
                                            TimeFeature::dayOfYear));
                            add(
                                    "T",
                                    Arrays.asList(
                                            TimeFeature::secondOfMinute,
                                            TimeFeature::minuteOfHour,
                                            TimeFeature::hourOfDay,
                                            TimeFeature::dayOfWeek,
                                            TimeFeature::dayOfMonth,
                                            TimeFeature::dayOfYear));
                        }
                    };

    public TimeFeature() {}

    public static NDArray secondOfMinute(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(LocalDateTime::getSecond).toArray())
                .toType(DataType.FLOAT32, false)
                .divi(59f)
                .subi(0.5);
    }

    public static NDArray minuteOfHour(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(LocalDateTime::getMinute).toArray())
                .toType(DataType.FLOAT32, false)
                .divi(59f)
                .subi(0.5);
    }

    public static NDArray hourOfDay(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(LocalDateTime::getHour).toArray())
                .toType(DataType.FLOAT32, false)
                .divi(23f)
                .subi(0.5);
    }

    public static NDArray dayOfWeek(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(a -> a.getDayOfWeek().ordinal()).toArray())
                .toType(DataType.FLOAT32, false)
                .divi(6f)
                .subi(0.5);
    }

    public static NDArray dayOfMonth(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(LocalDateTime::getDayOfMonth).toArray())
                .toType(DataType.FLOAT32, false)
                .subi(1f)
                .divi(30f)
                .subi(0.5f);
    }

    public static NDArray dayOfYear(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(LocalDateTime::getDayOfYear).toArray())
                .toType(DataType.FLOAT32, false)
                .subi(1f)
                .divi(365f)
                .subi(0.5f);
    }

    public static NDArray monthOfYear(NDManager manager, List<LocalDateTime> index) {
        return manager.create(index.stream().mapToInt(LocalDateTime::getMonthValue).toArray())
                .toType(DataType.FLOAT32, false)
                .subi(1f)
                .divi(11f)
                .subi(0.5);
    }

    public static NDArray weekOfYear(NDManager manager, List<LocalDateTime> index) {
        throw new UnsupportedOperationException("weekOfYear is not supported yet");
    }

    /**
     * Returns a list of time features that will be appropriate for the given frequency string.
     *
     * @param freqStr Frequency string of the form [multiple][granularity] such as "12H", "1D"
     * @return time features
     */
    public static List<BiFunction<NDManager, List<LocalDateTime>, NDArray>> timeFeaturesFromFreqStr(
            String freqStr) {
        return FEATURES_BY_OFFSETS.get(freqStr);
    }
}
