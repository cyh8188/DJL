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
package ai.djl;

import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.djl.util.Utils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code BaseModel} is the basic implementation of {@link Model}. */
public abstract class BaseModel implements Model {

    private static final Logger logger = LoggerFactory.getLogger(BaseModel.class);
    private static final int MODEL_VERSION = 1;
    protected Path modelDir;
    protected Block block;
    protected String modelName;
    protected NDManager manager;
    protected DataType dataType;
    protected PairList<String, Shape> inputData;
    protected Map<String, Object> artifacts = new ConcurrentHashMap<>();
    protected Map<String, String> properties = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public Block getBlock() {
        return block;
    }

    /** {@inheritDoc} */
    @Override
    public void setBlock(Block block) {
        this.block = block;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return modelName;
    }

    /** {@inheritDoc} */
    @Override
    public NDManager getNDManager() {
        return manager;
    }

    /** {@inheritDoc} */
    @Override
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getDataType() {
        return dataType;
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeInput() {
        if (inputData == null) {
            inputData = block.describeInput();
        }
        return inputData;
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeOutput() {
        List<String> names = inputData.keys();
        Shape[] outputShapes =
                block.getOutputShapes(
                        manager, inputData.values().toArray(new Shape[inputData.size()]));
        return new PairList<>(names, Arrays.asList(outputShapes));
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getArtifact(String name, Function<InputStream, T> function) throws IOException {
        try {
            Object artifact =
                    artifacts.computeIfAbsent(
                            name,
                            v -> {
                                try (InputStream is = getArtifactAsStream(name)) {
                                    return function.apply(is);
                                } catch (IOException e) {
                                    throw new IllegalStateException(e);
                                }
                            });
            return (T) artifact;
        } catch (RuntimeException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public URL getArtifact(String artifactName) throws IOException {
        if (artifactName == null) {
            throw new IllegalArgumentException("artifactName cannot be null");
        }
        Path file = modelDir.resolve(artifactName);
        if (Files.exists(file) && Files.isReadable(file)) {
            return file.toUri().toURL();
        }
        throw new FileNotFoundException("File not found: " + file);
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getArtifactAsStream(String name) throws IOException {
        URL url = getArtifact(name);
        return url.openStream();
    }

    /** {@inheritDoc} */
    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    protected void setModelDir(Path modelDir) {
        this.modelDir = modelDir;
    }

    protected void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /** {@inheritDoc} */
    @Override
    public void save(Path modelPath, String modelName) throws IOException {
        if (Files.notExists(modelPath)) {
            Files.createDirectories(modelPath);
        }

        if (block == null || !block.isInitialized()) {
            throw new IllegalStateException("Model has not be trained or loaded yet.");
        }

        String epochValue = getProperty("Epoch");
        int epoch =
                epochValue == null
                        ? Utils.getCurrentEpoch(modelPath, modelName) + 1
                        : Integer.parseInt(epochValue);

        Path paramFile = modelPath.resolve(String.format("%s-%04d.params", modelName, epoch));
        try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(paramFile))) {
            dos.writeBytes("DJL@");
            dos.writeInt(MODEL_VERSION);
            dos.writeUTF(modelName);
            dos.writeUTF(dataType.name());
            inputData = block.describeInput();
            dos.writeInt(inputData.size());
            for (Pair<String, Shape> desc : inputData) {
                String name = desc.getKey();
                if (name == null) {
                    dos.writeUTF("");
                } else {
                    dos.writeUTF(name);
                }
                dos.write(desc.getValue().getEncoded());
            }

            dos.writeInt(properties.size());
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }

            block.saveParameters(dos);
        }
        this.modelName = modelName;
        modelDir = modelPath.toAbsolutePath();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        if (manager.isOpen()) {
            logger.warn("Model was not closed explicitly.");
            manager.close();
        }
        super.finalize();
    }

    protected Path paramPathResolver(Map<String, String> options) throws IOException {
        Path paramFile;
        if (Files.isRegularFile(modelDir)) {
            paramFile = modelDir;
        } else {
            String epochOption = null;
            if (options != null) {
                epochOption = options.get("epoch");
            }
            int epoch;
            if (epochOption == null) {
                epoch = Utils.getCurrentEpoch(modelDir, modelName);
                if (epoch == -1) {
                    throw new IOException(
                            "Parameter file not found in: "
                                    + modelDir
                                    + ". If you only specified model path, make sure path name match"
                                    + "your saved model file name.");
                }
            } else {
                epoch = Integer.parseInt(epochOption);
            }

            paramFile = modelDir.resolve(String.format("%s-%04d.params", modelName, epoch));
        }
        return paramFile;
    }

    protected boolean readParameters(Map<String, String> options)
            throws IOException, MalformedModelException {
        Path paramFile = paramPathResolver(options);
        logger.debug("Try to load model from {}", paramFile);
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(paramFile))) {
            byte[] buf = new byte[4];
            dis.readFully(buf);
            if (!"DJL@".equals(new String(buf, StandardCharsets.US_ASCII))) {
                return false;
            }

            int version = dis.readInt();
            if (version != MODEL_VERSION) {
                throw new IOException("Unsupported model version: " + version);
            }

            modelName = dis.readUTF();
            logger.debug("Loading model parameter: {}", modelName);

            dataType = DataType.valueOf(dis.readUTF());

            int numberOfInputs = dis.readInt();
            inputData = new PairList<>();
            for (int i = 0; i < numberOfInputs; ++i) {
                String inputName = dis.readUTF(); // input name
                Shape shape = Shape.decode(dis);
                inputData.add(inputName, shape);
            }

            int numberOfProperties = dis.readInt();
            for (int i = 0; i < numberOfProperties; ++i) {
                String key = dis.readUTF();
                String value = dis.readUTF();
                properties.put(key, value);
            }

            block.loadParameters(manager, dis);
            logger.debug("DJL model loaded successfully");
        }
        return true;
    }
}
