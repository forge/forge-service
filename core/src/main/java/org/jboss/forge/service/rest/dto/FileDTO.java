/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.rest.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@XmlRootElement
public class FileDTO
{
   private final boolean container;
   private final String name;
   private final String path;

   @XmlElementWrapper(name = "children")
   private final List<FileDTO> children = new ArrayList<>();

   /**
    * @param container
    * @param name
    * @param path
    */
   public FileDTO(boolean container, String name, String path)
   {
      super();
      this.container = container;
      this.name = name;
      this.path = path;
   }

   /**
    * @return the container
    */
   public boolean isContainer()
   {
      return container;
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @return the children
    */
   public List<FileDTO> getChildren()
   {
      return children;
   }

   /**
    * @return the path
    */
   public String getPath()
   {
      return path;
   }

   @Override
   public String toString()
   {
      return "FileDTO [container=" + container + ", name=" + name + ", path=" + path + ", children=" + children + "]";
   }
}
