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

import java.util.Collections;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextListener;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.CommandControllerFactory;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.service.spi.ResourceProvider;
import org.jboss.forge.service.ui.RestUIContext;
import org.jboss.forge.service.ui.RestUIRuntime;
import org.jboss.forge.service.util.StringUtils;
import org.jboss.forge.service.util.UICommandHelper;

@Path("/forge")
public class CommandsResource
{
   @Inject
   private CommandFactory commandFactory;

   @Inject
   private CommandControllerFactory controllerFactory;

   @Inject
   private Iterable<UIContextListener> contextListeners;

   @Inject
   private UICommandHelper helper;

   @Inject
   private ResourceProvider resourceProvider;

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public JsonObject getInfo()
   {
      return createObjectBuilder()
               .add("version", Versions.getImplementationVersionFor(UIContext.class).toString())
               .build();
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
                              .add("id", shellifiedName)
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
      try (CommandController controller = createCommandController(name, resource))
      {
         helper.describeController(builder, controller);
      }
      return builder.build();
   }

   @POST
   @Path("/command/{name}/validate")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public JsonObject validateCommand(@PathParam("name") String name, JsonObject content)
            throws Exception
   {
      String resource = content.getString("resource");
      JsonObjectBuilder builder = createObjectBuilder();
      try (CommandController controller = createCommandController(name, resource))
      {
         helper.populateControllerAllInputs(content, controller);
         helper.describeCurrentState(builder, controller);
         helper.describeValidation(builder, controller);
         helper.describeInputs(builder, controller);
      }
      return builder.build();
   }

   @POST
   @Path("/command/{name}/next")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public JsonObject nextStep(@PathParam("name") String name, JsonObject content)
            throws Exception
   {
      String resource = content.getString("resource");
      int stepIndex = content.getInt("stepIndex", 1);
      JsonObjectBuilder builder = createObjectBuilder();
      try (CommandController controller = createCommandController(name, resource))
      {
         if (!(controller instanceof WizardCommandController))
         {
            throw new WebApplicationException("Controller is not a wizard", Status.BAD_REQUEST);
         }
         WizardCommandController wizardController = (WizardCommandController) controller;
         helper.populateController(content, wizardController);
         for (int i = 0; i < stepIndex; i++)
         {
            if (wizardController.canMoveToNextStep())
            {
               wizardController.next().initialize();
               helper.populateController(content, wizardController);
            }
         }
         helper.describeMetadata(builder, controller);
         helper.describeCurrentState(builder, controller);
         helper.describeValidation(builder, controller);
         helper.describeInputs(builder, controller);
      }
      return builder.build();
   }

   @POST
   @Path("/command/{name}/execute")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public JsonObject executeCommand(@PathParam("name") String name, JsonObject content)
            throws Exception
   {
      String resource = content.getString("resource");
      JsonObjectBuilder builder = createObjectBuilder();
      try (CommandController controller = createCommandController(name, resource))
      {
         helper.populateControllerAllInputs(content, controller);
         helper.describeValidation(builder, controller);
         helper.describeExecution(builder, controller);
      }
      return builder.build();
   }

   private RestUIContext createUIContext(String resource)
   {
      Resource<?> selection = resourceProvider.toResource(resource);
      return new RestUIContext(selection, contextListeners);
   }

   private CommandController createCommandController(String name, String resource) throws Exception
   {
      RestUIContext context = createUIContext(resource);
      // As the name is shellified, it needs to be false
      context.getProvider().setGUI(false);
      UICommand command = commandFactory.getCommandByName(context, name);
      if (command == null)
      {
         throw new WebApplicationException(Status.NOT_FOUND);
      }
      context.getProvider().setGUI(true);
      CommandController controller = controllerFactory.createController(context,
               new RestUIRuntime(Collections.emptyList()), command);
      controller.initialize();
      return controller;
   }
}
