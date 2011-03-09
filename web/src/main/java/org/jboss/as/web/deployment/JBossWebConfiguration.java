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

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.as.web.Enhancer;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.merge.web.jboss.JBossWebMetaDataMerger;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.ErrorPageMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.WelcomeFileListMetaData;
import org.jboss.msc.service.ServiceName;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom JBossAS web configuration.
 * Leave defaults as they are.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JBossWebConfiguration extends AbstractConfiguration implements Enhancer, ServletContextHandler.Decorator {

    private WarMetaData warMetaData;
    private Enhancer enhancer;

    public JBossWebConfiguration(WarMetaData warMetaData) {
        this.warMetaData = warMetaData;
    }

    public void setEnhancer(Enhancer enhancer) {
        this.enhancer = enhancer;
    }

    @Override
    public void preConfigure(WebAppContext context) throws Exception {
        // let override it for the moment -- see SharedWebMetaDataBuilder
        // default
        JBossWebMetaData sharedWebMetaData = new JBossWebMetaData();
        JBossWebMetaDataMerger.merge(sharedWebMetaData, null, warMetaData.getSharedWebMetaData());
        processWebMetaData(context, sharedWebMetaData);
    }

    @Override
    public void configure(WebAppContext context) throws Exception {
        // Add decorator -- ugly impl details
        ContextHandler.Context servletContext = ContextHandler.getCurrentContext();
        ServletContextHandler contextHandler = (ServletContextHandler) servletContext.getContextHandler();
        contextHandler.addDecorator(this);
        // custom
        processWebMetaData(context, warMetaData.getMergedJBossWebMetaData());
    }

    @SuppressWarnings({"unchecked"})
    protected void processWebMetaData(WebAppContext context, JBossWebMetaData metaData) throws Exception {
        ServletHandler servletHandler = context.getServletHandler();

        // Display name
        DescriptionGroupMetaData dg = metaData.getDescriptionGroup();
        if (dg != null) {
            String displayName = dg.getDisplayName();
            if (displayName != null) {
                context.setDisplayName(displayName);
            }
        }

        // Distributable
        if (metaData.getDistributable() != null)
            context.setDistributable(true);

        // Context params
        List<ParamValueMetaData> contextParams = metaData.getContextParams();
        if (contextParams != null) {
            for (ParamValueMetaData param : contextParams) {
                context.getInitParams().put(param.getParamName(), param.getParamValue());
            }
        }

        // Error pages
        List<ErrorPageMetaData> errorPages = metaData.getErrorPages();
        if (errorPages != null) {
            Map<String, String> errors = new HashMap<String, String>();
            for (ErrorPageMetaData value : errorPages) {
                String error = value.getErrorCode();
                if (error == null || error.length() == 0)
                    error = value.getExceptionType();
                String location = value.getLocation();
                errors.put(error, location);
            }
            if (context.getErrorHandler() instanceof ErrorPageErrorHandler)
                ((ErrorPageErrorHandler) context.getErrorHandler()).setErrorPages(errors);
        }

        // Filter definitions
        FiltersMetaData filters = metaData.getFilters();
        if (filters != null) {
            for (FilterMetaData value : filters) {
                FilterHolder filterHolder = new FilterHolder();
                filterHolder.setName(value.getFilterName());
                filterHolder.setClassName(value.getFilterClass());
                if (value.getInitParam() != null)
                    for (ParamValueMetaData param : value.getInitParam()) {
                        filterHolder.setInitParameter(param.getParamName(), param.getParamValue());
                    }
                servletHandler.addFilter(filterHolder);
            }
        }

        // Filter mappings
        List<FilterMappingMetaData> filtersMappings = metaData.getFilterMappings();
        if (filtersMappings != null) {
            for (FilterMappingMetaData value : filtersMappings) {
                FilterMapping filterMapping = new FilterMapping();
                filterMapping.setFilterName(value.getFilterName());
                List<String> servletNames = value.getServletNames();
                if (servletNames != null) {
                    filterMapping.setServletNames(servletNames.toArray(new String[servletNames.size()]));
                }
                List<String> urlPatterns = value.getUrlPatterns();
                if (urlPatterns != null) {
                    filterMapping.setPathSpecs(urlPatterns.toArray(new String[urlPatterns.size()]));
                }
                List<DispatcherType> dispatchers = value.getDispatchers();
                if (dispatchers != null) {
                    int dispatch = FilterMapping.DEFAULT;
                    for (DispatcherType type : dispatchers)
                        dispatch |= type.ordinal();
                    filterMapping.setDispatches(dispatch);
                }
                servletHandler.addFilterMapping(filterMapping);
            }
        }

        // Listeners
        List<ListenerMetaData> listeners = metaData.getListeners();
        if (listeners != null) {
            for (ListenerMetaData value : listeners) {
                context.addEventListener((EventListener) newInstance(context, value.getListenerClass()));
            }
        }

        // Login configuration
        LoginConfigMetaData loginConfig = metaData.getLoginConfig();
        if (loginConfig != null) {
           // TODO
        }

        // MIME mappings
        List<MimeMappingMetaData> mimes = metaData.getMimeMappings();
        if (mimes != null) {
            for (MimeMappingMetaData value : mimes) {
                context.getMimeTypes().addMimeMapping(value.getExtension(), value.getMimeType());
            }
        }

        // Security constraints
        List<SecurityConstraintMetaData> scs = metaData.getSecurityConstraints();
        if (scs != null) {
           // TODO
        }

        // Security roles
        SecurityRolesMetaData roles = metaData.getSecurityRoles();
        if (roles != null) {
           // TODO
        }

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

        // Welcome files
        WelcomeFileListMetaData welcomeFiles = metaData.getWelcomeFileList();
        if (welcomeFiles != null) {
            List<String> files = welcomeFiles.getWelcomeFiles();
            if (files != null)
                context.setWelcomeFiles(files.toArray(new String[files.size()]));
        }

        // Session timeout
        SessionConfigMetaData scmd = metaData.getSessionConfig();
        if (scmd != null) {
            int timeout = scmd.getSessionTimeout();
            context.getSessionHandler().getSessionManager().setMaxInactiveInterval(timeout * 60);
        }
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T newInstance(WebAppContext context, String className) throws Exception {
        return (T) create(context.loadClass(className));
    }

    @Override
    public <T> T create(Class<T> clazz) throws Exception {
        return (enhancer != null) ? enhancer.create(clazz) : clazz.newInstance();
    }

    @Override
    public <T> T enhance(T instance) {
        return (enhancer != null) ? enhancer.enhance(instance) : instance;
    }

    @Override
    public void destroy(Object instance) {
        if (enhancer != null)
            enhancer.destroy(instance);
    }

    @Override
    public ServiceName getServiceName() {
        return null;  // No need for the name
    }

    @Override
    public <T extends Filter> T decorateFilterInstance(T filter) throws ServletException {
        return enhance(filter);
    }

    @Override
    public <T extends Servlet> T decorateServletInstance(T servlet) throws ServletException {
        return enhance(servlet);
    }

    @Override
    public <T extends EventListener> T decorateListenerInstance(T listener) throws ServletException {
        return enhance(listener);
    }

    @Override
    public void decorateFilterHolder(FilterHolder filter) throws ServletException {
    }

    @Override
    public void decorateServletHolder(ServletHolder servlet) throws ServletException {
    }

    @Override
    public void destroyServletInstance(Servlet s) {
        destroy(s);
    }

    @Override
    public void destroyFilterInstance(Filter f) {
        destroy(f);
    }

    @Override
    public void destroyListenerInstance(EventListener f) {
        destroy(f);
    }
}
