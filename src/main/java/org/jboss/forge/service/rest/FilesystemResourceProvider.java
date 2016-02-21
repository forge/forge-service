/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.service.spi.ResourceProvider;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class FilesystemResourceProvider implements ResourceProvider
{
   private static Path rootPath;

   static
   {
      // TODO: Move to external configuration
      rootPath = Paths.get(System.getenv().getOrDefault("OPENSHIFT_DATA_DIR",
               "/tmp"), "workspace");
      if (!Files.exists(rootPath))
      {
         try
         {
            Files.createDirectory(rootPath);
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
   }

   @Inject
   private ResourceFactory resourceFactory;

   @Override
   public Resource<?> toResource(String path)
   {
      return resourceFactory.create(rootPath.resolve(path).toFile());
   }

}
