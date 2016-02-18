/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.rest;

import java.io.File;

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

   @Inject
   private ResourceFactory resourceFactory;

   @Override
   public Resource<?> toResource(String path)
   {
      return resourceFactory.create(new File(path));
   }

}
