/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2014 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.impl.v1.service.content;

import org.apache.commons.lang.StringUtils;
import org.craftercms.studio.api.v1.constant.CStudioConstants;
import org.craftercms.studio.api.v1.exception.ContentNotFoundException;
import org.craftercms.studio.api.v1.exception.ServiceException;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.service.ConfigurableServiceBase;
import org.craftercms.studio.api.v1.service.configuration.ContentTypesConfig;
import org.craftercms.studio.api.v1.service.configuration.ServicesConfig;
import org.craftercms.studio.api.v1.service.content.ContentService;
import org.craftercms.studio.api.v1.service.content.ContentTypeService;
import org.craftercms.studio.api.v1.to.ContentItemTO;
import org.craftercms.studio.api.v1.to.ContentTypeConfigTO;
import org.craftercms.studio.api.v1.to.TimeStamped;

import java.io.Serializable;
import java.util.*;

/**
 * @author Dejan Brkic
 */
public class ContentTypeServiceImpl extends ConfigurableServiceBase implements ContentTypeService {

    private static final Logger logger = LoggerFactory.getLogger(ContentTypeServiceImpl.class);

    @Override
    public ContentTypeConfigTO getContentTypeForContent(String site, String path) throws ServiceException {
        ContentItemTO itemTO = contentService.getContentItem(site, path);
        if (itemTO != null) {
            String type = itemTO.getContentType();
            if (!StringUtils.isEmpty(type)) {
                return servicesConfig.getContentTypeConfig(site, type);
                } else {
                    throw new ServiceException("No content type specified for " + path + " in site: " + site);
                }
            } else {
                throw new ContentNotFoundException(path + " is not found in site: " + site);
            }
    }

    @Override
    public boolean isUserAllowed(Set<String> userRoles, ContentTypeConfigTO item) {
        if (item != null) {
            String name = item.getName();
            Set<String> allowedRoles = item.getAllowedRoles();
            logger.debug("Checking allowed roles on " + name + ". user roles: "
                        + userRoles + ", allowed roles: " + allowedRoles);

            if (allowedRoles == null || allowedRoles.size() == 0) {
                return true;
            } else {
                boolean notAllowed = Collections.disjoint(userRoles, allowedRoles);
                if (notAllowed) {
                    logger.debug(name + " is not allowed for the user.");
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            logger.debug("no content type config provided. returning true for user access to content type checking.");

            return true;
        }
    }

    @Override
    public ContentTypeConfigTO getContentTypeByRelativePath(String site, String relativePath) throws ServiceException {
        ContentItemTO item = contentService.getContentItem(site, relativePath);
        if (item != null) {
            String type = item.getContentType();
            if (!StringUtils.isEmpty(type)) {
                return servicesConfig.getContentTypeConfig(site, type);
            } else {
                throw new ServiceException("No content type specified for " + relativePath + " in site: " + site);
            }
        } else {
            throw new ContentNotFoundException(relativePath + " is not found in site: " + site );
        }
    }

    @Override
    public ContentTypeConfigTO getContentType(String site, String type) {
        return servicesConfig.getContentTypeConfig(site, type);
    }

    @Override
    public List<ContentTypeConfigTO> getAllContentTypes(String site, boolean searchable) {
        return getAllContentTypes(site);
    }

    protected List<ContentTypeConfigTO> getAllContentTypes(String site) {
        ContentItemTO folders = contentService.getContentItemTree(_configPath.replaceAll(CStudioConstants.PATTERN_SITE, site), 1);
        List<ContentTypeConfigTO> contentTypes = new ArrayList<>();
        if (folders != null) {
            for (ContentItemTO rootFolderInfo : folders.getChildren()) {
                // traverse the children file-folder structure
                getContentTypeConfigForChildren(site, rootFolderInfo, contentTypes);
            }
        }
        return contentTypes;
    }

    /**
     * Traverse file folder -- recursive!, searching for config.xml
     *
     * @param site
     * @param node
     */
    protected void getContentTypeConfigForChildren(String site, ContentItemTO node, List<ContentTypeConfigTO> contentTypes) {
        logger.debug("Get Content Type Config fot Children path = {0}", node.getUri());
        ContentItemTO folders = contentService.getContentItemTree(node.getUri(), 1);
        if (folders != null) {
            for (ContentItemTO folderInfo : folders.getChildren()) {
                if (folderInfo.isFolder()) {
                    ContentItemTO configNode = contentService.getContentItem(folderInfo.getUri() + "/" + _configFileName);
                    if (configNode != null){
                        ContentTypeConfigTO config = contentTypesConfig.loadConfiguration(site, configNode);
                        if (config != null) {
                            contentTypes.add(config);
                        }
                    }
                    // traverse the children file-folder structure

                    getContentTypeConfigForChildren(site, folderInfo, contentTypes);
                }
            }
        }
    }

    @Override
    protected TimeStamped getConfiguration(String key) {
        // not used
        return null;
    }

    @Override
    protected void removeConfiguration(String key) {
        // not used
    }

    @Override
    protected void loadConfiguration(String key) {
        // not used
    }

    @Override
    public void register() {
        getServicesManager().registerService(ContentTypeService.class, this);
    }

    public ContentService getContentService() { return contentService; }
    public void setContentService(ContentService contentService) { this.contentService = contentService; }

    public ServicesConfig getServicesConfig() { return servicesConfig; }
    public void setServicesConfig(ServicesConfig servicesConfig) { this.servicesConfig = servicesConfig; }

    public ContentTypesConfig getContentTypesConfig() { return contentTypesConfig; }
    public void setContentTypesConfig(ContentTypesConfig contentTypesConfig) { this.contentTypesConfig = contentTypesConfig; }

    protected ContentService contentService;
    protected ServicesConfig servicesConfig;
    protected ContentTypesConfig contentTypesConfig;
}
