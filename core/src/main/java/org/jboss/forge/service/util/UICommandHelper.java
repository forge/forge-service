/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.util;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.ManyValued;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.SingleValued;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.output.UIMessage;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.InputComponents;

/**
 * Describes commands
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class UICommandHelper
{
   private final ConverterFactory converterFactory;

   @Inject
   public UICommandHelper(ConverterFactory converterFactory)
   {
      this.converterFactory = converterFactory;
   }

   public void describeController(JsonObjectBuilder builder, CommandController controller)
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
      describeValidation(builder, controller);
      describeInputs(builder, controller);
   }

   @SuppressWarnings("unchecked")
   public void describeInputs(JsonObjectBuilder builder, CommandController controller)
   {
      // Add inputs
      JsonArrayBuilder inputBuilder = createArrayBuilder();
      for (InputComponent<?, ?> input : controller.getInputs().values())
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

   public void describeValidation(JsonObjectBuilder builder, CommandController controller)
   {
      // Add messages
      JsonArrayBuilder messages = createArrayBuilder();
      for (UIMessage message : controller.validate())
      {
         JsonObjectBuilder messageObj = createObjectBuilder()
                  .add("description", message.getDescription())
                  .add("severity", message.getSeverity().name());
         if (message.getSource() != null)
            messageObj.add("input", message.getSource().getName());
         messages.add(messageObj);
      }
      builder.add("messages", messages);
   }

   public void describeExecution(JsonObjectBuilder builder, CommandController controller) throws Exception
   {
      Result result = controller.execute();
      describeResult(builder, result);
   }

   public void populateController(JsonObject content, CommandController controller)
   {
      JsonArray inputArray = content.getJsonArray("inputs");
      for (int i = 0; i < inputArray.size(); i++)
      {
         JsonObject input = inputArray.getJsonObject(i);
         String inputName = input.getString("name");
         String inputValue = input.getString("value");
         if (controller.hasInput(inputName))
            controller.setValueFor(inputName, inputValue);
      }
   }

   /**
    * @param builder
    * @param result
    */
   public void describeResult(JsonObjectBuilder builder, Result result)
   {
      JsonArrayBuilder array = createArrayBuilder();
      if (result instanceof CompositeResult)
      {
         for (Result r : ((CompositeResult) result).getResults())
         {
            array.add(_describeResult(createObjectBuilder(), r));
         }
      }
      else
      {
         array.add(_describeResult(createObjectBuilder(), result));
      }
      builder.add("result", array);
   }

   private JsonObjectBuilder _describeResult(JsonObjectBuilder builder, Result result)
   {
      builder.add("status", (result instanceof Failed) ? "FAILED" : "SUCCESS");
      addOptional(builder, "message", result.getMessage());
      return builder;
   }

   private void addOptional(JsonObjectBuilder builder, String name, Object value)
   {
      if (value != null)
      {
         builder.add(name, value.toString());
      }
   }

}