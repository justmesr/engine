/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.UserSettings;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.UserSettingsFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User settings service tests.
 */
@RunWith(Arquillian.class)
public class UserSettingsServiceIntegrationTest extends IntegrationTestBase {

   @Inject
   private UserSettingsFacade userSettingsFacade;

   @Inject
   @SystemDataStorage
   DataStorage dataStorage;

   @Inject
   DataStorageDialect dataStorageDialect;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   private final String TARGET_URI = "http://localhost:8080";
   private final String PATH_PREFIX = PATH_CONTEXT + "/rest/settings/user/";

   @Before
   public void init() throws Exception {
      dataStorage.dropDocument(LumeerConst.UserSettings.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropDocument(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropDocument(LumeerConst.Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void readUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org1", "Organization"));
      organizationFacade.setOrganizationCode("org1");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org1", "proj1"));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(PATH_PREFIX)
            .request(MediaType.APPLICATION_JSON)
            .buildGet()
            .invoke();

      UserSettings userSettings = response.readEntity(UserSettings.class);
      assertThat(userSettings).isNotNull();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org1");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");
   }

   @Test
   public void upsertUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org11", "Organization"));
      organizationFacade.createOrganization(new Organization("org13", "Organization"));
      organizationFacade.setOrganizationCode("org11");
      projectFacade.createProject(new Project("proj1", "Project"));
      organizationFacade.setOrganizationCode("org13");
      projectFacade.createProject(new Project("projXYZ", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org11", "proj1"));

      UserSettings userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org11");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildPut(Entity.json(new UserSettings("org13", null)))
                   .invoke();

      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org11");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildPut(Entity.json(new UserSettings("org13", "projXYZ")))
                   .invoke();

      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org13");
      assertThat(userSettings.getDefaultProject()).isEqualTo("projXYZ");
   }

   @Test
   public void organizationDoesntExistTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org21", "Organization"));
      organizationFacade.setOrganizationCode("org21");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org21", "proj1"));

      Response response = ClientBuilder.newBuilder().build()
                                      .target(TARGET_URI)
                                      .path(PATH_PREFIX)
                                      .request(MediaType.APPLICATION_JSON)
                                      .buildPut(Entity.json(new UserSettings("org23", "projXYZ")))
                                      .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
   }

   @Test
   public void projectDoesntExistTest() throws Exception{
      organizationFacade.createOrganization(new Organization("org31", "Organization"));
      organizationFacade.setOrganizationCode("org31");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org31", "proj1"));

      Response response = ClientBuilder.newBuilder().build()
                                       .target(TARGET_URI)
                                       .path(PATH_PREFIX)
                                       .request(MediaType.APPLICATION_JSON)
                                       .buildPut(Entity.json(new UserSettings("org31", "projXYZ")))
                                       .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

   }


}
