/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.viewer.restfulobjects.server.mappers;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import org.jboss.resteasy.spi.Failure;

import org.apache.isis.applib.RecoverableException;
import org.apache.isis.viewer.restfulobjects.applib.JsonRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.RepresentationType;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse;
import org.apache.isis.viewer.restfulobjects.rendering.ExceptionWithBody;
import org.apache.isis.viewer.restfulobjects.rendering.ExceptionWithHttpStatusCode;
import org.apache.isis.viewer.restfulobjects.server.mappers.entity.ExceptionDetail;
import org.apache.isis.viewer.restfulobjects.server.mappers.entity.ExceptionPojo;
import org.apache.isis.viewer.restfulobjects.server.resources.serialization.SerializationStrategy;

public abstract class ExceptionMapperAbstract<T extends Throwable> implements ExceptionMapper<T> {

    @Context
    protected HttpHeaders httpHeaders;

    Response buildResponse(final T ex) {
        final RestfulResponse.HttpStatusCode httpStatusCode = determineStatusCode(ex);
        final String message = messageFor(ex);

        if(ex instanceof ExceptionWithBody) {
            final ExceptionWithBody exceptionWithBody = (ExceptionWithBody) ex;
            final JsonRepresentation body = exceptionWithBody.getBody();
            if(body != null) {
                return buildResponse(httpStatusCode, message, body);
            }
        }

        final ExceptionPojo exceptionPojo =
                new ExceptionPojo(
                        httpStatusCode.getStatusCode(), message,
                        detailIfRequired(httpStatusCode, ex)
                );

        return buildResponse(httpStatusCode, exceptionPojo);
    }

    private Response buildResponse(
            final RestfulResponse.HttpStatusCode httpStatusCode,
            final String message,
            final JsonRepresentation body) {
        final ResponseBuilder builder = Response.status(httpStatusCode.getJaxrsStatusType());
        if (message != null) {
            builder.header(RestfulResponse.Header.WARNING.getName(), RestfulResponse.Header.WARNING.render(message));
        }

        final SerializationStrategy serializationStrategy = SerializationStrategy.JSON;

        // hmm; the mediaType doesn't seem to be specified in the RO spec
        builder.type(serializationStrategy.type(RepresentationType.GENERIC));
        builder.entity(body.toString());

        return builder.build();
    }

    private RestfulResponse.HttpStatusCode determineStatusCode(final T ex) {
        final List<Throwable> chain = Throwables.getCausalChain(ex);

        RestfulResponse.HttpStatusCode statusCode;
        if(ex instanceof Failure) {
            Failure failure = (Failure) ex;
            statusCode = RestfulResponse.HttpStatusCode.statusFor(failure.getErrorCode());
        } else if(!FluentIterable.from(chain).filter(RecoverableException.class).isEmpty()) {
            statusCode = RestfulResponse.HttpStatusCode.OK;
        } else if(ex instanceof ExceptionWithHttpStatusCode) {
            ExceptionWithHttpStatusCode exceptionWithHttpStatusCode = (ExceptionWithHttpStatusCode) ex;
            statusCode = exceptionWithHttpStatusCode.getHttpStatusCode();
        } else {
            statusCode = RestfulResponse.HttpStatusCode.INTERNAL_SERVER_ERROR;
        }
        return statusCode;
    }

    private static String messageFor(final Throwable ex) {
        final List<Throwable> chain = Throwables.getCausalChain(ex);
        final Optional<RecoverableException> recoverableIfAny =
                FluentIterable.from(chain).filter(RecoverableException.class).first();
        return (recoverableIfAny.isPresent() ? recoverableIfAny.get() : ex).getMessage();
    }

    private ExceptionDetail detailIfRequired(
            final RestfulResponse.HttpStatusCode httpStatusCode,
            final Throwable ex) {
        return httpStatusCode == RestfulResponse.HttpStatusCode.NOT_FOUND ||
                httpStatusCode == RestfulResponse.HttpStatusCode.OK
                ? null
                : new ExceptionDetail(ex);
    }

    private Response buildResponse(
            final RestfulResponse.HttpStatusCode httpStatusCode,
            final ExceptionPojo exceptionPojo) {
        final ResponseBuilder builder = Response.status(httpStatusCode.getJaxrsStatusType());

        final List<MediaType> acceptableMediaTypes = httpHeaders.getAcceptableMediaTypes();
        final SerializationStrategy serializationStrategy =
                acceptableMediaTypes.contains(MediaType.APPLICATION_XML_TYPE) ||
                        acceptableMediaTypes.contains(RepresentationType.OBJECT_LAYOUT.getXmlMediaType())
                        ? SerializationStrategy.XML
                        : SerializationStrategy.JSON;

        final String message = exceptionPojo.getMessage();
        if (message != null) {
            builder.header(RestfulResponse.Header.WARNING.getName(), RestfulResponse.Header.WARNING.render(message));
        }

        builder.type(serializationStrategy.type(RepresentationType.ERROR));
        builder.entity(serializationStrategy.entity(exceptionPojo));

        return builder.build();
    }


}
