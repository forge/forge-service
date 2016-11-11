/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.service.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

import java.io.File;

import org.jboss.forge.furnace.impl.util.Files;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@org.junit.Ignore("Need to setup environment first")
public class CommandResourceTest
{
   @BeforeClass
   public static void setUp()
   {
      RestAssured.baseURI = "http://localhost:8080/forge-service/api/forge";
   }

   @Test
   public void testInfo() throws Exception
   {
      get("").then().assertThat().body("version", equalTo("3.0.0-SNAPSHOT"));
   }

   @Test
   public void testProjectNewIsAvailable() throws Exception
   {
      get("/commands").then().assertThat()
               .body("name", hasItem("Project: New"));
   }

   @Test
   public void testCommandIs404() throws Exception
   {
      get("/command/foocommand").then().assertThat()
               .statusCode(404);
   }

   @Test
   public void testProjectNewInfo() throws Exception
   {
      get("/command/project-new").then().assertThat()
               .body("name", equalTo("Project: New"));
   }

   @Test
   public void testProjectNewValidate() throws Exception
   {
      given()
               .body("{\"resource\": \"/tmp\",\"inputs\": [{\"name\":\"named\", \"value\": \"foo\"}]}")
               .contentType(ContentType.JSON)
               .when()
               .post("/command/project-new/validate")
               .then()
               .body("canExecute", equalTo(true));

   }

   @Test
   public void testProjectNewExecute() throws Exception
   {
      File projectRoot = new File("/tmp/foo");
      Files.delete(projectRoot, true);
      Assert.assertFalse(projectRoot.isDirectory());
      given().body("{\"resource\": \"/tmp\",\"inputs\": [{\"name\":\"named\", \"value\": \"foo\"}]}")
               .contentType(ContentType.JSON)
               .when().post("/command/project-new/execute")
               .then()
               .assertThat().body("result[0].status", equalTo("SUCCESS"))
               .body("result[0].message", equalTo("Project named 'foo' has been created."));
      Assert.assertTrue(projectRoot.isDirectory());
   }

}
