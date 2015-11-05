/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.jboss.forge.service.rest;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import java.io.File;
import java.util.Collections;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextListener;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.CommandControllerFactory;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.service.ui.RestUIContext;
import org.jboss.forge.service.ui.RestUIRuntime;
import org.jboss.forge.service.util.StringUtils;
import org.jboss.forge.service.util.CommandDescriber;

@Path("/api/forge")
public class CommandsResource
{
   @Inject
   private CommandFactory commandFactory;

   @Inject
   private CommandControllerFactory controllerFactory;

   @Inject
   private ResourceFactory resourceFactory;

   @Inject
   private Iterable<UIContextListener> contextListeners;

   @Inject
   private CommandDescriber commands;

   @GET
   public String getInfo()
   {
      return Versions.getImplementationVersionFor(UIContext.class).toString();
   }

   @GET
   @Path("/commands")
   @Produces(MediaType.APPLICATION_JSON)
   public JsonArray getCommandNames(@QueryParam("resource") String resource)
   {
      try (RestUIContext context = createUIContext(resource))
      {
         JsonArrayBuilder arrayBuilder = createArrayBuilder();

         for (String command : commandFactory.getEnabledCommandNames(context))
         {
            String shellifiedName = StringUtils.shellifyCommandName(command);
            arrayBuilder.add(
                     createObjectBuilder()
                              .add("name", command)
                              .add("href", "/api/forge/command/" + shellifiedName));
         }
         return arrayBuilder.build();
      }
   }

   @GET
   @Path("/command/{name}")
   @Produces(MediaType.APPLICATION_JSON)
   public JsonObject getCommandInfo(@QueryParam("resource") String resource, @PathParam("name") String name)
            throws Exception
   {
      JsonObjectBuilder builder = createObjectBuilder();
      try (RestUIContext context = createUIContext(resource))
      {
         // As the name is shellified, it needs to be false
         context.getProvider().setGUI(false);
         UICommand command = commandFactory.getCommandByName(context, name);
         if (command == null)
         {
            throw new WebApplicationException(Status.NOT_FOUND);
         }
         context.getProvider().setGUI(true);
         try (CommandController controller = controllerFactory.createController(context,
                  new RestUIRuntime(Collections.emptyList()), command))
         {
            controller.initialize();
            commands.describeController(builder, controller);
         }
      }
      return builder.build();
   }

   private RestUIContext createUIContext(String resource)
   {
      // TODO: Change this
      Resource<File> selection = resourceFactory.create(OperatingSystemUtils.getTempDirectory());
      return new RestUIContext(selection, contextListeners);
   }
}
