/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.rest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.forge.service.rest.dto.FileDTO;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@javax.ws.rs.Path("/filesystem")
@ApplicationScoped
public class FilesystemResource
{
   private Path rootPath;

   @PostConstruct
   public void init()
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

   @GET
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public FileDTO getFilesystemStructure() throws Exception
   {
      final FileDTO[] result = new FileDTO[1];
      final Deque<FileDTO> stack = new LinkedList<>();
      Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>()
      {
         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
         {
            FileDTO dto = new FileDTO(true, dir.toFile().getName(), rootPath.relativize(dir).toString());
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
            FileDTO dto = new FileDTO(false, file.toFile().getName(), rootPath.relativize(file).toString());
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
   public String contents(@QueryParam("resource") String fileName) throws Exception
   {
      Path path = rootPath.resolve(fileName);
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

   @GET
   @javax.ws.rs.Path("/zip")
   @Produces("application/zip")
   public Response zip(@QueryParam("resource") String fileName) throws Exception
   {
      Path path = rootPath.resolve(fileName);
      if (!Files.exists(path))
         throw new WebApplicationException(404);
      Path tmpFile = Files.createTempFile("dld", ".zip");
      Files.delete(tmpFile);
      try
      {
         create(tmpFile, path);
         byte[] contents = Files.readAllBytes(tmpFile);
         return Response.ok(contents)
                  .header("Content-Disposition", "inline; filename=\"project.zip\"")
                  .build();
      }
      finally
      {
         Files.delete(tmpFile);
      }
   }

   /**
    * Creates/updates a zip file.
    * 
    * @param zipFilename the name of the zip to create
    * @param filenames list of filename to add to the zip
    * @throws IOException
    */
   private void create(Path zipFilename, Path... filenames)
            throws IOException
   {

      try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, true))
      {
         final Path root = zipFileSystem.getPath("/");

         // iterate over the files we need to add
         for (final Path src : filenames)
         {
            // add a file to the zip file system
            if (!Files.isDirectory(src))
            {
               final Path dest = zipFileSystem.getPath(root.toString(),
                        rootPath.relativize(src).toString());
               final Path parent = dest.getParent();
               if (Files.notExists(parent))
               {
                  System.out.printf("Creating directory %s\n", parent);
                  Files.createDirectories(parent);
               }
               Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            else
            {
               // for directories, walk the file tree
               Files.walkFileTree(src, new SimpleFileVisitor<Path>()
               {
                  @Override
                  public FileVisitResult visitFile(Path file,
                           BasicFileAttributes attrs) throws IOException
                  {
                     final Path dest = zipFileSystem.getPath(root.toString(),
                              rootPath.relativize(file).toString());
                     Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                     return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult preVisitDirectory(Path dir,
                           BasicFileAttributes attrs) throws IOException
                  {
                     final Path dirToCreate = zipFileSystem.getPath(root.toString(),
                              rootPath.relativize(dir).toString());
                     if (Files.notExists(dirToCreate))
                     {
                        System.out.printf("Creating directory %s\n", dirToCreate);
                        Files.createDirectories(dirToCreate);
                     }
                     return FileVisitResult.CONTINUE;
                  }
               });
            }
         }
      }
   }

   /**
    * Returns a zip file system
    * 
    * @param zipFilename to construct the file system from
    * @param create true if the zip file should be created
    * @return a zip file system
    * @throws IOException
    */
   private static FileSystem createZipFileSystem(Path path,
            boolean create)
                     throws IOException
   {
      final URI uri = URI.create("jar:file:" + path.toUri().getPath());

      final Map<String, String> env = new HashMap<>();
      if (create)
      {
         env.put("create", "true");
      }
      return FileSystems.newFileSystem(uri, env);
   }

}
