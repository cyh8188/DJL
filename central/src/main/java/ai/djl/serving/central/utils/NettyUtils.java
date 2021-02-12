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
package ai.djl.serving.central.utils;

import ai.djl.ModelException;
import ai.djl.modality.Input;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A utility class that handling Netty request and response. */
public final class NettyUtils {

    private static final Logger logger = LoggerFactory.getLogger("NettyUtils");

    private NettyUtils() {}

    /**
     * Sends the json object to client.
     *
     * @param ctx the connection context
     * @param json the json object
     */
    public static void sendJsonResponse(ChannelHandlerContext ctx, Object json) {
        sendJsonResponse(ctx, JsonUtils.GSON_PRETTY.toJson(json), HttpResponseStatus.OK);
    }

    /**
     * Sends the json string to client with specified status.
     *
     * @param ctx the connection context
     * @param json the json string
     * @param status the HTTP status
     */
    public static void sendJsonResponse(
            ChannelHandlerContext ctx, Object json, HttpResponseStatus status) {
        sendJsonResponse(ctx, JsonUtils.GSON_PRETTY.toJson(json), status);
    }

    /**
     * Sends the json string to client.
     *
     * @param ctx the connection context
     * @param json the json string
     */
    public static void sendJsonResponse(ChannelHandlerContext ctx, String json) {
        sendJsonResponse(ctx, json, HttpResponseStatus.OK);
    }



    /**
     * Returns the bytes for the specified {@code ByteBuf}.
     *
     * @param buf the {@code ByteBuf} to read
     * @return the bytes for the specified {@code ByteBuf}
     */
    public static byte[] getBytes(ByteBuf buf) {
        if (buf.hasArray()) {
            return buf.array();
        }

        byte[] ret = new byte[buf.readableBytes()];
        int readerIndex = buf.readerIndex();
        buf.getBytes(readerIndex, ret);
        return ret;
    }

    /**
     * Reads the parameter's value for the key from the uri.
     *
     * @param decoder the {@code QueryStringDecoder} parsed from uri
     * @param key the parameter key
     * @param def the default value
     * @return the parameter's value
     */
    public static String getParameter(QueryStringDecoder decoder, String key, String def) {
        List<String> param = decoder.parameters().get(key);
        if (param != null && !param.isEmpty()) {
            return param.get(0);
        }
        return def;
    }

    /**
     * Read the parameter's integer value for the key from the uri.
     *
     * @param decoder the {@code QueryStringDecoder} parsed from uri
     * @param key the parameter key
     * @param def the default value
     * @return the parameter's integer value
     * @throws NumberFormatException exception is thrown when the parameter-value is not numeric.
     */
    public static int getIntParameter(QueryStringDecoder decoder, String key, int def) {
        String value = getParameter(decoder, key, null);
        if (value == null || value.isEmpty()) {
            return def;
        }
        return Integer.parseInt(value);
    }

    /**
     * Parses form data and added to the {@link Input} object.
     *
     * @param data the form data
     * @param input the {@link Input} object to be added to
     */
    public static void addFormData(InterfaceHttpData data, Input input) {
        if (data == null) {
            return;
        }
        try {
            String name = data.getName();
            switch (data.getHttpDataType()) {
                case Attribute:
                    Attribute attribute = (Attribute) data;
                    input.addData(name, attribute.getValue().getBytes(StandardCharsets.UTF_8));
                    break;
                case FileUpload:
                    FileUpload fileUpload = (FileUpload) data;
                    input.addData(name, getBytes(fileUpload.getByteBuf()));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Except form field, but got " + data.getHttpDataType());
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
