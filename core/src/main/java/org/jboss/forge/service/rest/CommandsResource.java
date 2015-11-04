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

import java.io.File;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextListener;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.service.ui.RestUIContext;

@Path("/api/forge")
@Stateless
public class CommandsResource
{
   @Inject
   private CommandFactory commandFactory;

   @Inject
   private ResourceFactory resourceFactory;

   @Inject
   private Iterable<UIContextListener> contextListeners;

   @GET
   public String getInfo()
   {
      return Versions.getImplementationVersionFor(UIContext.class).toString();
   }

   @GET
   @Path("/commandNames")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getCommandNames(@QueryParam("resource") String resource)
   {
      try (RestUIContext context = createUIContext(resource))
      {
         return commandFactory.getCommandNames(context);
      }
   }

   private RestUIContext createUIContext(String resource)
   {
      // TODO: Change this
      Resource<File> selection = resourceFactory.create(OperatingSystemUtils.getTempDirectory());
      return new RestUIContext(selection, contextListeners);
   }
}
