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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.engine.api.dto.Config;
import io.lumeer.engine.controller.configuration.ConfigurationManipulator;
import io.lumeer.engine.controller.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.util.Resources;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Manipulates user configuration properties.
 */
@SessionScoped
public class ConfigurationFacade implements Serializable {

   public enum ConfigurationLevel {
      USER_GLOBAL, USER_PROJECT, USER_ORGANIZATION, PROJECT, ORGANIZATION
   }

   private static Logger log = Resources.produceLog(ConfigurationLevel.class.getName());

   protected static final String USER_CONFIG_COLLECTION = "_config_user";
   protected static final String PROJECT_CONFIG_COLLECTION = "_config_project";
   protected static final String ORGANIZATION_CONFIG_COLLECTION = "_config_organization";

   @Inject
   private UserFacade userFacade;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured data storage.
    */
   public StorageConnection getDataStorage() {
      return new StorageConnection(
            getConfigurationString(LumeerConst.DB_HOST_PROPERTY).orElse("localhost"),
            getConfigurationInteger(LumeerConst.DB_PORT_PROPERTY).orElse(27017),
            getConfigurationString(LumeerConst.DB_USER_PROPERTY).orElse("pepa"),
            getConfigurationString(LumeerConst.DB_PASSWORD_PROPERTY).orElse(""));
   }

   public String getDataStorageDatabase() {
      return getConfigurationString(LumeerConst.DB_NAME_PROPERTY).orElse("lumeer");
   }

   public Boolean getDataStorageUseSsl() {
      return Boolean.valueOf(getConfigurationString(LumeerConst.DB_USE_SSL).orElse("false"));
   }

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured system data storage.
    */
   public StorageConnection getSystemDataStorage() {
      return new StorageConnection(
            defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_HOST_PROPERTY),
            Integer.valueOf(defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_PORT_PROPERTY)),
            defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_USER_PROPERTY),
            defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_PASSWORD_PROPERTY));
   }

   public String getSystemDataStorageDatabase() {
      return defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_NAME_PROPERTY);
   }

   public Boolean getSystemDataStorageUseSsl() {
      return Boolean.valueOf(defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_USE_SSL));
   }

   /**
    * Returns an Optional String value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getConfigurationString(final String key) {
      return createOptionalString(getConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getConfigurationInteger(final String key) {
      return createOptionalInteger(getConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getConfigurationDocument(final String key) {
      return createOptionalDataDocument(getConfiguration(key));
   }

   /**
    * Returns an Optional String value of the given key for currently logged user configuration.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getUserConfigurationString(final ConfigurationLevel levelIn, final String key) {
      return createOptionalString(getUserConfiguration(levelIn, key));
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getUserConfigurationInteger(final ConfigurationLevel levelIn, final String key) {
      return createOptionalInteger(getUserConfiguration(levelIn, key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getUserConfigurationDocument(final ConfigurationLevel levelIn, final String key) {
      return createOptionalDataDocument(getUserConfiguration(levelIn, key));
   }

   /**
    * Returns an Optional String value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getProjectConfigurationString(final String key) {
      return createOptionalString(getProjectConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getProjectConfigurationInteger(final String key) {
      return createOptionalInteger(getProjectConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getProjectConfigurationDocument(final String key) {
      return createOptionalDataDocument(getProjectConfiguration(key));
   }

   /**
    * Returns an Optional String value of the given key for currently logged organization configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getOrganizationConfigurationString(final String key) {
      return createOptionalString(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged organization configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getOrganizationConfigurationInteger(final String key) {
      return createOptionalInteger(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged organization configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getOrganizationConfigurationDocument(final String key) {
      return createOptionalDataDocument(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Config value of the given key for user. It will return null, if the value of the given key does not exist.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Config value of the given key
    */
   public Config getUserConfiguration(final ConfigurationLevel levelIn, final String key) {
      String configName = userConfigName(levelIn, organizationFacade.getOrganizationId(), projectFacade.getCurrentProjectId(), userFacade.getUserEmail());
      return configurationManipulator.getConfiguration(USER_CONFIG_COLLECTION, configName, key);
   }

   /**
    * Returns an List of user configurations at specified level
    *
    * @param levelIn
    *       specify type of parent resource
    * @return List of configurations
    */
   public List<Config> getUserConfigurations(final ConfigurationLevel levelIn) {
      String configName = userConfigName(levelIn, organizationFacade.getOrganizationId(), projectFacade.getCurrentProjectId(), userFacade.getUserEmail());
      return configurationManipulator.getConfigurations(USER_CONFIG_COLLECTION, configName);
   }

   /**
    * Returns an Config value of the given key for project. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Config value of the given key
    */
   public Config getProjectConfiguration(final String key) {
      String configName = createConfigName(organizationFacade.getOrganizationId(), projectFacade.getCurrentProjectId());
      return configurationManipulator.getConfiguration(PROJECT_CONFIG_COLLECTION, configName, key);
   }

   /**
    * Returns an List of project configurations
    *
    * @return List of configurations
    */
   public List<Config> getProjectConfigurations() {
      String configName = createConfigName(organizationFacade.getOrganizationId(), projectFacade.getCurrentProjectId());
      return configurationManipulator.getConfigurations(PROJECT_CONFIG_COLLECTION, configName);
   }

   /**
    * Returns an Config value of the given key for organization. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   public Config getOrganizationConfiguration(final String key) {
      return configurationManipulator.getConfiguration(ORGANIZATION_CONFIG_COLLECTION, organizationFacade.getOrganizationId(), key);
   }

   /**
    * Returns an List of organization configurations
    *
    * @return List of configurations
    */
   public List<Config> getOrganizationConfigurations() {
      return configurationManipulator.getConfigurations(ORGANIZATION_CONFIG_COLLECTION, organizationFacade.getOrganizationId());
   }

   /**
    * Sets a new key-value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param config
    *       config object
    */
   public void setUserConfiguration(final ConfigurationLevel levelIn, final Config config) {
      setConfiguration(levelIn, config);
   }

   /**
    * Sets a new key-values for currently logged user.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param configs
    *       configuration objects
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setUserConfigurations(final ConfigurationLevel levelIn, final List<Config> configs, final boolean reset) {
      setConfigurations(levelIn, configs, reset);
   }

   /**
    * Sets a new key-value to configuration entry for active project. If the given key exists, its value will be updated.
    *
    * @param config
    *       config object
    */
   public void setProjectConfiguration(final Config config) {
      setConfiguration(ConfigurationLevel.PROJECT, config);
   }

   /**
    * Sets a new key-values for currently logged user.
    *
    * @param configs
    *       configuration objects
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setProjectConfigurations(final List<Config> configs, final boolean reset) {
      setConfigurations(ConfigurationLevel.PROJECT, configs, reset);
   }

   /**
    * Sets a new key-value to configuration entry for currently logged organization. If the given key exists, its value will be updated.
    *
    * @param config
    *       config object
    */
   public void setOrganizationConfiguration(final Config config) {
      setConfiguration(ConfigurationLevel.ORGANIZATION, config);
   }

   /**
    * Sets a new key-values for currently logged user.
    *
    * @param configs
    *       configuration objects
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setOrganizationConfigurations(final List<Config> configs, final boolean reset) {
      setConfigurations(ConfigurationLevel.ORGANIZATION, configs, reset);
   }

   /**
    * Resets currently logged user configuration. The whole user configuration entry will not be deleted from the system collection, just the config field!
    *
    * @param levelIn
    *       specify type of parent resource
    */
   public void resetUserConfiguration(final ConfigurationLevel levelIn) {
      resetConfiguration(levelIn);
   }

   /**
    * Removes the specified attribute located in user configuration entry.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetUserConfigurationAttribute(final ConfigurationLevel levelIn, final String attributeName) {
      resetConfigurationAttribute(levelIn, attributeName);
   }

   /**
    * Resets active project configuration. The whole project configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetProjectConfiguration() {
      resetConfiguration(ConfigurationLevel.PROJECT);
   }

   /**
    * Removes the specified attribute located in project configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetProjectConfigurationAttribute(final String attributeName) {
      resetConfigurationAttribute(ConfigurationLevel.PROJECT, attributeName);
   }

   /**
    * Resets currently logged organization configuration. The whole organization configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetOrganizationConfiguration() {
      resetConfiguration(ConfigurationLevel.ORGANIZATION);
   }

   /**
    * Removes the specified attribute located in organization configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetOrganizationConfigurationAttribute(final String attributeName) {
      resetConfigurationAttribute(ConfigurationLevel.ORGANIZATION, attributeName);
   }

   /**
    * Resets the configuration of specified group (user or organization).
    *
    * @param level
    *       configuration level
    */
   private void resetConfiguration(final ConfigurationLevel level) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, user);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, userOrgConfigName);
            break;
         case USER_PROJECT:
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, userProjConfigName);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.resetConfiguration(PROJECT_CONFIG_COLLECTION, projectConfigName);
            break;
         case ORGANIZATION:
            configurationManipulator.resetConfiguration(ORGANIZATION_CONFIG_COLLECTION, organization);
            break;
      }
   }

   /**
    * Resets the configuration attribute of specified group (user or organization).
    *
    * @param level
    *       configuration level
    * @param attributeName
    *       configuration attribute name
    */
   private void resetConfigurationAttribute(final ConfigurationLevel level, final String attributeName) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, user, attributeName);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, userOrgConfigName, attributeName);
            break;
         case USER_PROJECT:
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, userProjConfigName, attributeName);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.resetConfigurationAttribute(PROJECT_CONFIG_COLLECTION, projectConfigName, attributeName);
            break;
         case ORGANIZATION:
            configurationManipulator.resetConfigurationAttribute(ORGANIZATION_CONFIG_COLLECTION, organization, attributeName);
            break;
      }

   }

   /**
    * Returns an Config value of the given key by priority (User, Organization, Project).
    *
    * @param key
    *       the name of key
    * @return Config value of the given key
    */
   private Config getConfiguration(final String key) {
      Config projectConfig = getProjectConfiguration(key);
      boolean projectRestricted = projectConfig != null && projectConfig.isRestricted();
      Config organizationConfig = getOrganizationConfiguration(key);
      boolean organizationRestricted = organizationConfig != null && organizationConfig.isRestricted();

      Config userConfigProject = getUserConfiguration(ConfigurationLevel.USER_PROJECT, key);
      if (userConfigProject != null && !projectRestricted && !organizationRestricted) {
         return userConfigProject;
      }

      if (projectConfig != null && !organizationRestricted) {
         return projectConfig;
      }

      Config userConfigOrganization = getUserConfiguration(ConfigurationLevel.USER_ORGANIZATION, key);
      if (userConfigOrganization != null && !organizationRestricted) {
         return userConfigOrganization;
      }

      if (organizationConfig != null) {
         return organizationConfig;
      }

      Config userConfigGlobal = getUserConfiguration(ConfigurationLevel.USER_GLOBAL, key);
      if (userConfigGlobal != null) {
         return userConfigGlobal;
      }

      return new Config(key, defaultConfigurationProducer.get(key));
   }

   /**
    * Sets a new key-Object value to the specified configuration entry. If the given key exists, the value of specified field will be updated.
    *
    * @param level
    *       configuration level
    * @param config
    *       configuration object
    */
   private void setConfiguration(final ConfigurationLevel level, final Config config) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, user, config);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, userOrgConfigName, config);
            break;
         case USER_PROJECT:
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, userProjConfigName, config);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.setConfiguration(PROJECT_CONFIG_COLLECTION, projectConfigName, config);
            break;
         case ORGANIZATION:
            configurationManipulator.setConfiguration(ORGANIZATION_CONFIG_COLLECTION, organization, config);
            break;
      }

   }

   private void setConfigurations(final ConfigurationLevel level, final List<Config> configs, final boolean reset) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.setConfigurations(USER_CONFIG_COLLECTION, user, configs, reset);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.setConfigurations(USER_CONFIG_COLLECTION, userOrgConfigName, configs, reset);
            break;
         case USER_PROJECT:
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.setConfigurations(USER_CONFIG_COLLECTION, userProjConfigName, configs, reset);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.setConfigurations(PROJECT_CONFIG_COLLECTION, projectConfigName, configs, reset);
            break;
         case ORGANIZATION:
            configurationManipulator.setConfigurations(ORGANIZATION_CONFIG_COLLECTION, organization, configs, reset);
            break;
      }

   }

   private String userConfigName(ConfigurationLevel level, String organization, String project, String user) {
      String userConfigName;
      switch (level) {
         case USER_PROJECT:
            userConfigName = createConfigName(organization, project, user);
            break;
         case USER_ORGANIZATION:
            userConfigName = createConfigName(organization, user);
            break;
         default:
            userConfigName = createConfigName(user);
      }
      return userConfigName;
   }

   private static String createConfigName(String... args) {
      if (args.length == 0) {
         return "";
      }
      String val = args[0];
      for (int i = 1; i < args.length; i++) {
         val += "/" + args[i];
      }
      return val;
   }

   private static Optional<DataDocument> createOptionalDataDocument(Config config) {
      return config != null && config.getValue() instanceof DataDocument ? Optional.of((DataDocument) config.getValue()) : Optional.empty();
   }

   private static Optional<String> createOptionalString(Config config) {
      return config != null && config.getValue() != null ? Optional.of(config.getValue().toString()) : Optional.empty();
   }

   private static Optional<Integer> createOptionalInteger(Config config) {
      if (config == null || config.getValue() == null) {
         return Optional.empty();
      }

      try {
         return Optional.of(Integer.parseInt(config.getValue().toString()));
      } catch (NumberFormatException ex) {
         log.log(Level.FINEST, "Cannot get integer configuration value", ex);
         return Optional.empty();
      }
   }

}
