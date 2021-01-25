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
package ai.djl.serving.wlm;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author erik.bamberg@web.de
 *
 */
public class WorkerIDGenerator {

    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger(1);

    public int generate() {
	return WORKER_COUNTER.getAndIncrement();
    }
    
}
