/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import ai.djl.inference.Predictor;
import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.translate.TranslateException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkerThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WorkerThread.class);

    private ModelInfo model;
    private Predictor<Input, Output> predictor;

    private AtomicBoolean running = new AtomicBoolean(true);

    private BatchAggregator aggregator;
    private int gpuId;
    private AtomicReference<Thread> currentThread = new AtomicReference<>();
    private WorkerState state;
    private int workerId;
    private long startTime;
    private boolean fixPoolThread;

    public WorkerThread(
            int gpuId, ModelInfo model, BatchAggregator aggregator, boolean fixPoolThread) {
        this.model = model;
        this.aggregator = aggregator;
        this.gpuId = gpuId;
        this.workerId = new WorkerIdGenerator().generate();
        this.startTime = System.currentTimeMillis();
        predictor = model.getModel().newPredictor();
        this.fixPoolThread = fixPoolThread;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        thread.setName(getWorkerName());
        currentThread.set(thread);
        this.state = WorkerState.WORKER_STARTED;
        List<Input> req = null;
        try {
            while (isRunning() && !aggregator.isFinished()) {
                req = aggregator.getRequest();
                if (req != null && !req.isEmpty()) {
                    try {
                        List<Output> reply = predictor.batchPredict(req);
                        aggregator.sendResponse(reply);
                    } catch (TranslateException e) {
                        logger.warn("Failed to predict", e);
                        aggregator.sendError();
                    }
                }
                req = null;
            }

        } catch (InterruptedException e) {
            logger.debug("Shutting down the thread .. Scaling down.");
        } catch (Throwable t) {
            logger.error("Server error", t);
        } finally {
            logger.debug("Shutting down worker thread .. {}", currentThread.get().getName());
            currentThread.set(null);
            shutdown(WorkerState.WORKER_STOPPED);
            if (req != null) {
                aggregator.sendError();
            }
        }
    }

    public int getWorkerId() {
        return workerId;
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getGpuId() {
        return gpuId;
    }

    public long getStartTime() {
        return startTime;
    }

    public WorkerState getState() {
        return state;
    }

    public void shutdown(WorkerState state) {
        running.set(false);
        setState(state);
        Thread thread = currentThread.getAndSet(null);
        if (thread != null) {
            thread.interrupt();
            aggregator.sendError();
        }
        predictor.close();
    }

    private String getWorkerName() {
        String modelName = model.getModelName();
        if (modelName.length() > 25) {
            modelName = modelName.substring(0, 25);
        }
        return "W-" + modelName + '-' + workerId;
    }

    void setState(WorkerState newState) {
        logger.debug("{} State change {} -> {}", getWorkerName(), state, newState);
        if (state != WorkerState.WORKER_SCALED_DOWN) {
            // Don't update the state if it was terminated on purpose.. Scaling in..
            this.state = newState;
        }
    }

    /**
     * check if this worker is instantiate is one of the fix threads of a pool. fix threads are not
     * automatically scales down, so they are candidate for down scaling when minWorker/maxWorker
     * size of a model changes.
     *
     * @return the fixPoolThread
     */
    public boolean isFixPoolThread() {
        return fixPoolThread;
    }
}
