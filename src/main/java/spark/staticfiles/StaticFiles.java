/*
 * Copyright 2015 - Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spark.staticfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.resource.AbstractFileResolvingResource;
import spark.resource.AbstractResourceHandler;
import spark.resource.ClassPathResource;
import spark.resource.ClassPathResourceHandler;
import spark.resource.ExternalResource;
import spark.resource.ExternalResourceHandler;
import spark.utils.Assert;
import spark.utils.IOUtils;

/**
 * Holds the static file configuration.
 *
 * TODO: Cache-Control and ETAG
 * TODO: Is global state a problem here?
 */
public class StaticFiles {
    private static final Logger LOG = LoggerFactory.getLogger(StaticFiles.class);

    private static List<AbstractResourceHandler> staticResourceHandlers = null;

    private static boolean staticResourcesSet = false;
    private static boolean externalStaticResourcesSet = false;

    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();

    static {
        CONTENT_TYPES.put("svg", "image/svg+xml");
        CONTENT_TYPES.put("css", "text/css");
        CONTENT_TYPES.put("js", "application/x-javascript");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
    }

    /**
     * @return true if consumed, false otherwise.
     */
    public static boolean consume(HttpServletRequest httpRequest,
                                  ServletResponse servletResponse) throws IOException {
        if (staticResourceHandlers != null) {
            for (AbstractResourceHandler staticResourceHandler : staticResourceHandlers) {
                AbstractFileResolvingResource resource = staticResourceHandler.getResource(httpRequest);
                if (resource != null && resource.isReadable()) {
                    setContentTypeFromFilename(servletResponse, httpRequest);
                    IOUtils.copy(resource.getInputStream(), servletResponse.getOutputStream());
                    return true;
                }
            }
        }
        return false;
    }

    public static void setContentTypeFromFilename(ServletResponse response, HttpServletRequest httpRequest) {
        String uri = httpRequest.getRequestURI();
        int dotLocation = uri.lastIndexOf(".");
        if (dotLocation >= 0 && dotLocation < (uri.length() - 1)) {
            String fileExtension = uri.substring(dotLocation + 1);
            String contentType = CONTENT_TYPES.get(fileExtension);
            if (contentType != null) {
                response.setContentType(contentType);
            }
        }
    }

    /**
     * Clears all static file configuration
     */
    public static void clear() {
        if (staticResourceHandlers != null) {
            staticResourceHandlers.clear();
            staticResourceHandlers = null;
        }
        staticResourcesSet = false;
        externalStaticResourcesSet = false;
    }

    /**
     * Configures location for static resources
     *
     * @param folder the location
     */
    public static void configureStaticResources(String folder) {
        Assert.notNull(folder, "'folder' must not be null");

        if (!staticResourcesSet) {
            try {
                ClassPathResource resource = new ClassPathResource(folder);
                if (!resource.getFile().isDirectory()) {
                    LOG.error("Static resource location must be a folder");
                    return;
                }

                if (staticResourceHandlers == null) {
                    staticResourceHandlers = new ArrayList<>();
                }
                staticResourceHandlers.add(new ClassPathResourceHandler(folder, "index.html"));
                LOG.info("StaticResourceHandler configured with folder = " + folder);
            } catch (IOException e) {
                LOG.error("Error when creating StaticResourceHandler", e);
            }
            staticResourcesSet = true;
        }

    }

    /**
     * Configures location for static resources
     *
     * @param folder the location
     */
    public static void configureExternalStaticResources(String folder) {
        Assert.notNull(folder, "'folder' must not be null");

        if (!externalStaticResourcesSet) {
            try {
                ExternalResource resource = new ExternalResource(folder);
                if (!resource.getFile().isDirectory()) {
                    LOG.error("External Static resource location must be a folder");
                    return;
                }

                if (staticResourceHandlers == null) {
                    staticResourceHandlers = new ArrayList<>();
                }
                staticResourceHandlers.add(new ExternalResourceHandler(folder, "index.html"));
                LOG.info("External StaticResourceHandler configured with folder = " + folder);
            } catch (IOException e) {
                LOG.error("Error when creating external StaticResourceHandler", e);
            }
            externalStaticResourcesSet = true;
        }

    }

}
