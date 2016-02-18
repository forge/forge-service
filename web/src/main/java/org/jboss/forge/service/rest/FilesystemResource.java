/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.rest;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Deque;
import java.util.LinkedList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.forge.service.rest.dto.FileDTO;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@javax.ws.rs.Path("/filesystem")
public class FilesystemResource
{
   @GET
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public FileDTO getFilesystemStructure() throws Exception
   {
      // TODO: Move to external configuration
      Path rootPath = Paths.get(System.getenv().getOrDefault("OPENSHIFT_DATA_DIR",
               "/tmp"), "workspace");
      final FileDTO[] result = new FileDTO[1];
      final Deque<FileDTO> stack = new LinkedList<>();
      Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>()
      {
         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
         {
            FileDTO dto = new FileDTO(true, dir.toFile().getName(), dir.toFile().getAbsolutePath());
            if (!stack.isEmpty())
            {
               stack.peek().getChildren().add(dto);
            }
            stack.push(dto);
            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
         {
            FileDTO dto = new FileDTO(false, file.toFile().getName(), file.toFile().getAbsolutePath());
            stack.peek().getChildren().add(dto);
            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
         {
            result[0] = stack.pop();
            return FileVisitResult.CONTINUE;
         }
      });
      return result[0];
   }

   @GET
   @javax.ws.rs.Path("/contents")
   @Produces(MediaType.TEXT_PLAIN)
   public String getContents(@QueryParam("resource") String fileName) throws Exception
   {
      Path path = Paths.get(fileName);
      if (Files.isRegularFile(path))
      {
         byte[] b = Files.readAllBytes(path);
         return new String(b);
      }
      else
      {
         return "Preview unavailable";
      }
   }
}
