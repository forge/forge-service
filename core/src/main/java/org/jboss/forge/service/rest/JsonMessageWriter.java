/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonMessageWriter implements MessageBodyWriter<JsonStructure>
{

   @Override
   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return type == JsonStructure.class;
   }

   @Override
   public long getSize(JsonStructure t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return 0;
   }

   @Override
   public void writeTo(JsonStructure t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                     throws IOException, WebApplicationException
   {
      JsonWriterFactory writerFactory = Json
               .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
      try (JsonWriter writer = writerFactory.createWriter(entityStream))
      {
         writer.write(t);
      }

   }

}
