/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.service.util;

import java.util.regex.Pattern;

/**
 * Copied from ShellUtils
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class StringUtils
{
   private static final Pattern WHITESPACES = Pattern.compile("\\W+");
   private static final Pattern COLONS = Pattern.compile("\\:");

   /**
    * "Shellifies" a name (that is, makes the name shell-friendly) by replacing spaces with "-" and removing colons
    *
    * @param name
    * @return
    */
   private static String shellifyName(String name)
   {
      return COLONS.matcher(WHITESPACES.matcher(name.trim()).replaceAll("-")).replaceAll("");
   }

   /**
    * Shellifies a command name
    * 
    * @param name
    * @return
    */
   public static String shellifyCommandName(String name)
   {
      return shellifyName(name).toLowerCase();
   }

}
