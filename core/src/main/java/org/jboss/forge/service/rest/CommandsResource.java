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
import java.util.Map;

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

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextListener;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.CommandControllerFactory;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.ManyValued;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.SingleValued;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.util.InputComponents;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.service.ui.RestUIContext;
import org.jboss.forge.service.ui.RestUIRuntime;
import org.jboss.forge.service.util.StringUtils;

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
   private ConverterFactory converterFactory;

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
            describeController(builder, controller);
         }
      }
      return builder.build();
   }

   /**
    * @param builder
    * @param controller
    */
   @SuppressWarnings("unchecked")
   private void describeController(JsonObjectBuilder builder, CommandController controller)
   {
      UICommandMetadata metadata = controller.getMetadata();
      builder.add("deprecated", metadata.isDeprecated());
      addOptional(builder, "category", metadata.getCategory());
      addOptional(builder, "name", metadata.getName());
      addOptional(builder, "description", metadata.getDescription());
      addOptional(builder, "deprecatedMessage", metadata.getDeprecatedMessage());
      builder.add("valid", controller.isValid());
      builder.add("canExecute", controller.canExecute());
      if (controller instanceof WizardCommandController)
      {
         builder.add("wizard", true);
         builder.add("canMoveToNextStep", ((WizardCommandController) controller).canMoveToNextStep());
         builder.add("canMoveToPreviousStep", ((WizardCommandController) controller).canMoveToPreviousStep());
      }
      else
      {
         builder.add("wizard", false);
      }
      Map<String, InputComponent<?, ?>> inputs = controller.getInputs();
      JsonArrayBuilder inputBuilder = createArrayBuilder();
      for (InputComponent<?, ?> input : inputs.values())
      {
         JsonObjectBuilder objBuilder = createObjectBuilder()
                  .add("name", input.getName())
                  .add("shortName", String.valueOf(input.getShortName()))
                  .add("valueType", input.getValueType().getName())
                  .add("inputType", InputComponents.getInputType(input))
                  .add("enabled", input.isEnabled())
                  .add("required", input.isRequired())
                  .add("label", InputComponents.getLabelFor(input, false));
         addOptional(objBuilder, "description", input.getDescription());
         addOptional(objBuilder, "note", input.getNote());
         Converter<Object, String> inputConverter = null;
         if (input instanceof SelectComponent)
         {
            SelectComponent<?, Object> selectComponent = (SelectComponent<?, Object>) input;
            inputConverter = InputComponents.getItemLabelConverter(converterFactory, selectComponent);
            JsonArrayBuilder valueChoices = createArrayBuilder();
            for (Object valueChoice : selectComponent.getValueChoices())
            {
               valueChoices.add(inputConverter.convert(valueChoice));
            }
            objBuilder.add("valueChoices", valueChoices);
         }
         if (inputConverter == null)
         {
            inputConverter = (Converter<Object, String>) converterFactory
                     .getConverter(input.getValueType(), String.class);
         }
         if (input instanceof ManyValued)
         {
            ManyValued<?, Object> many = (ManyValued<?, Object>) input;
            JsonArrayBuilder manyValues = createArrayBuilder();
            for (Object item : many.getValue())
            {
               manyValues.add(inputConverter.convert(item));
            }
            objBuilder.add("value", manyValues);
         }
         else
         {
            SingleValued<?, Object> single = (SingleValued<?, Object>) input;
            addOptional(objBuilder, "value", inputConverter.convert(single.getValue()));
         }
         inputBuilder.add(objBuilder);
      }
      builder.add("inputs", inputBuilder);
   }

   private void addOptional(JsonObjectBuilder builder, String name, Object value)
   {
      if (value != null)
      {
         builder.add(name, value.toString());
      }
   }

   private RestUIContext createUIContext(String resource)
   {
      // TODO: Change this
      Resource<File> selection = resourceFactory.create(OperatingSystemUtils.getTempDirectory());
      return new RestUIContext(selection, contextListeners);
   }
}
