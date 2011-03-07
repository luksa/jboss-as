/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.web.deployment;

import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;

import java.util.List;

/**
 * Custom JBossAS web configuration.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JBossWebConfiguration implements Configuration {

    private JBossWebMetaData metaData;
    private WebAppContext context;

    public JBossWebConfiguration(JBossWebMetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public void setWebAppContext(WebAppContext context) {
        this.context = context;
    }

    @Override
    public WebAppContext getWebAppContext() {
        return context;
    }

    @Override
    public void configureClassLoader() throws Exception {
        // do nothing, we already set this in processor
    }

    @Override
    public void configureDefaults() throws Exception {
    }

    @Override
    public void configureWebApp() throws Exception {

        ServletHandler servletHandler = getWebAppContext().getServletHandler();

        // Servlet
        JBossServletsMetaData servlets = metaData.getServlets();
        if (servlets != null) {
            for (JBossServletMetaData value : servlets) {
                ServletHolder servletHolder = new ServletHolder();
                servletHolder.setName(value.getServletName());
                servletHolder.setClassName(value.getServletClass());
                List<ParamValueMetaData> params = value.getInitParam();
                if (params != null) {
                    for (ParamValueMetaData param : params) {
                        servletHolder.setInitParameter(param.getParamName(), param.getParamValue());
                    }
                }
                servletHandler.addServlet(servletHolder);
            }

            // Servlet mapping
            List<ServletMappingMetaData> smappings = metaData.getServletMappings();
            if (smappings != null) {
                for (ServletMappingMetaData value : smappings) {
                    List<String> urlPatterns = value.getUrlPatterns();
                    if (urlPatterns != null) {
                        ServletMapping servletMapping = new ServletMapping();
                        servletMapping.setServletName(value.getServletName());
                        servletMapping.setPathSpecs(urlPatterns.toArray(new String[urlPatterns.size()]));
                        servletHandler.addServletMapping(servletMapping);
                    }
                }
            }
        }
    }

    @Override
    public void deconfigureWebApp() throws Exception {
    }
}
