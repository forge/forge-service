/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.util;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.input.HasCompleter;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.ManyValued;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.SingleValued;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.output.UIMessage;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.InputComponents;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.forge.service.ui.RestUIProvider;

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
      describeMetadata(builder, controller);
      describeCurrentState(builder, controller);
      describeInputs(builder, controller);
   }

   public void describeCurrentState(JsonObjectBuilder builder, CommandController controller)
   {
      JsonObjectBuilder stateBuilder = createObjectBuilder();
      stateBuilder.add("valid", controller.isValid());
      stateBuilder.add("canExecute", controller.canExecute());
      if (controller instanceof WizardCommandController)
      {
         stateBuilder.add("wizard", true);
         WizardCommandController wizardController = (WizardCommandController) controller;
         stateBuilder.add("canMoveToNextStep", wizardController.canMoveToNextStep());
         stateBuilder.add("canMoveToPreviousStep", wizardController.canMoveToPreviousStep());
         // Add flow
         JsonArrayBuilder wizardNamesStepBuilder = createArrayBuilder();
         List<UICommandMetadata> stepsMetadata = wizardController.getWizardStepsMetadata();
         for (UICommandMetadata metadata : stepsMetadata)
         {
            wizardNamesStepBuilder.add(metadata.getName());
         }
         stateBuilder.add("steps", wizardNamesStepBuilder);
      }
      else
      {
         stateBuilder.add("wizard", false);
      }
      builder.add("state", stateBuilder);
   }

   public void describeMetadata(JsonObjectBuilder builder, CommandController controller)
   {
      UICommandMetadata metadata = controller.getMetadata();
      JsonObjectBuilder metadataBuilder = createObjectBuilder();

      metadataBuilder.add("deprecated", metadata.isDeprecated());
      addOptional(metadataBuilder, "category", metadata.getCategory());
      addOptional(metadataBuilder, "name", metadata.getName());
      addOptional(metadataBuilder, "description", metadata.getDescription());
      addOptional(metadataBuilder, "deprecatedMessage", metadata.getDeprecatedMessage());
      builder.add("metadata", metadataBuilder);
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
                  .add("deprecated", input.isDeprecated())
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
               Object itemUnwrapped = Proxies.unwrap(valueChoice);
               String label = inputConverter.convert(itemUnwrapped);
               valueChoices.add(describeValueChoice(label, itemUnwrapped));
            }
            objBuilder.add("valueChoices", valueChoices);
            if (input instanceof UISelectMany)
            {
               objBuilder.add("class", UISelectMany.class.getSimpleName());
            }
            else
            {
               objBuilder.add("class", UISelectOne.class.getSimpleName());
            }
         }
         else
         {
            if (input instanceof UIInputMany)
            {
               objBuilder.add("class", UIInputMany.class.getSimpleName());
            }
            else
            {
               objBuilder.add("class", UIInput.class.getSimpleName());
            }
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
               manyValues.add(inputConverter.convert(Proxies.unwrap(item)));
            }
            objBuilder.add("value", manyValues);
         }
         else
         {
            SingleValued<?, Object> single = (SingleValued<?, Object>) input;

            Object value = single.getValue();
            if (value != null && !(value instanceof Number) && !(value instanceof Boolean))
            {
               value = inputConverter.convert(Proxies.unwrap(value));
            }
            addOptional(objBuilder, "value", value);
         }
         if (input instanceof HasCompleter)
         {
            HasCompleter<?, Object> hasCompleter = (HasCompleter<?, Object>) input;
            UICompleter<Object> completer = hasCompleter.getCompleter();
            if (completer != null)
            {
               JsonArrayBuilder typeAheadData = createArrayBuilder();
               Iterable<Object> valueChoices = completer.getCompletionProposals(controller.getContext(),
                        (InputComponent<?, Object>) input,
                        "");
               for (Object valueChoice : valueChoices)
               {
                  typeAheadData.add(inputConverter.convert(Proxies.unwrap(valueChoice)));
               }
               objBuilder.add("typeAheadData", typeAheadData);
            }
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
      // Get out and err
      RestUIProvider provider = (RestUIProvider) controller.getContext().getProvider();
      builder.add("out", provider.getOut());
      builder.add("err", provider.getErr());
   }

   public void populateControllerAllInputs(JsonObject content, CommandController controller) throws Exception
   {
      populateController(content, controller);
      int stepIndex = content.getInt("stepIndex", 0);
      if (controller instanceof WizardCommandController)
      {
         WizardCommandController wizardController = (WizardCommandController) controller;
         for (int i = 0; i < stepIndex && wizardController.canMoveToNextStep(); i++)
         {
            wizardController.next().initialize();
            populateController(content, wizardController);
         }
      }
   }

   public void populateController(JsonObject content, CommandController controller)
   {
      JsonArray inputArray = content.getJsonArray("inputs");
      assert inputArray != null : "'inputs' array not found in JSON content";
      for (int i = 0; i < inputArray.size(); i++)
      {
         JsonObject input = inputArray.getJsonObject(i);
         String inputName = input.getString("name");
         JsonValue valueObj = input.get("value");
         Object inputValue = null;
         if (valueObj != null)
         {
            switch (valueObj.getValueType())
            {
            case ARRAY:
               ArrayList<String> list = new ArrayList<>();
               for (JsonValue value : (JsonArray) valueObj)
               {
                  if (value.getValueType() == ValueType.STRING)
                  {
                     list.add(((JsonString) value).getString());
                  }
               }
               inputValue = list;
               break;
            case FALSE:
               inputValue = false;
               break;
            case TRUE:
               inputValue = true;
               break;
            case NUMBER:
               inputValue = ((JsonNumber) valueObj).intValue();
               break;
            case STRING:
               inputValue = ((JsonString) valueObj).getString();
               break;
            default:
               break;
            }
         }
         if (controller.hasInput(inputName) && inputValue != null)
            controller.setValueFor(inputName, inputValue);
      }
   }

   public void describeResult(JsonObjectBuilder builder, Result result)
   {
      JsonArrayBuilder array = createArrayBuilder();
      collectResults(array, result);
      builder.add("results", array);
   }

   protected void collectResults(JsonArrayBuilder results, Result result)
   {
      if (result instanceof CompositeResult)
      {
         for (Result r : ((CompositeResult) result).getResults())
         {
            collectResults(results, r);
         }
      }
      else
      {
         results.add(describeSingleResult(result));
      }
   }

   protected JsonObjectBuilder describeValueChoice(String label, Object obj)
   {
      JsonObjectBuilder builder = createObjectBuilder();
      builder.add("id", label);
      if (!(obj instanceof String) && !(obj instanceof Number) && !(obj instanceof Boolean))
      {
         try
         {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors())
            {
               // Ignore class attribute from Object.class and do not override id
               String name = pd.getName();
               if ("id".equals(name) || "class".equals(name))
               {
                  continue;
               }
               Method readMethod = pd.getReadMethod();
               if (readMethod != null)
               {
                  try
                  {
                     addOptional(builder, name, readMethod.invoke(obj));
                  }
                  catch (Exception e)
                  {
                     // Ignore
                     // e.printStackTrace();
                  }
               }
            }
         }
         catch (IntrospectionException ie)
         {
            // ignore
            // ie.printStackTrace();
         }
      }
      return builder;
   }

   protected JsonObjectBuilder describeSingleResult(Result result)
   {
      JsonObjectBuilder builder = createObjectBuilder();
      builder.add("status", (result instanceof Failed) ? "FAILED" : "SUCCESS");
      if (result != null)
         addOptional(builder, "message", result.getMessage());
      return builder;
   }

   @SuppressWarnings("unchecked")
   protected void addOptional(JsonObjectBuilder builder, String name, Object value)
   {
      if (value != null)
      {
         if (value instanceof Boolean)
         {
            builder.add(name, (Boolean) value);
         }
         else if (value instanceof Number)
         {
            builder.add(name, ((Number) value).intValue());
         }
         else if (value instanceof Iterable)
         {
            JsonArrayBuilder arrayBuilder = createArrayBuilder();
            for (Object element : (Iterable<Object>) value)
            {
               if (element != null)
               {
                  arrayBuilder.add(element.toString());
               }
            }
            builder.add(name, arrayBuilder);
         }
         else
         {
            builder.add(name, value.toString());
         }
      }
   }

}