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
package org.jboss.forge.service.main;

import java.io.File;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import org.jboss.forge.service.producer.FurnaceProducer;

/**
 * Initializes Forge add-on repository
 */
public class ForgeInitializer
{
   private static final transient Logger LOG = Logger.getLogger(ForgeInitializer.class.getName());

   /**
    * Called when CDI is initialized
    */
   public void initialize(@Observes @Initialized(ApplicationScoped.class) Object init, FurnaceProducer furnaceProducer)
   {
      // lets ensure that the addons folder is initialized
      File repoDir = new File(
               "/home/ggastald/workspace/forge-core-2.0/dist/target/forge-distribution-3.0.0-SNAPSHOT/addons");
      LOG.info("initializing furnace with folder: " + repoDir.getAbsolutePath());
      File[] files = repoDir.listFiles();
      if (files == null || files.length == 0)
      {
         LOG.warning("No files found in the addon directory: " + repoDir.getAbsolutePath());
      }
      else
      {
         LOG.warning("Found " + files.length + " addon files in directory: " + repoDir.getAbsolutePath());
      }
      furnaceProducer.setup(repoDir);
   }

   public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init, FurnaceProducer furnaceProducer)
   {
      furnaceProducer.destroy();
   }
}
