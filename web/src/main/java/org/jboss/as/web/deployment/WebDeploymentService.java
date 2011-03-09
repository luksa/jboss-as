/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.as.web.WebServer;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service starting a web deployment.
 *
 * @author Emanuel Muckenhuber
 */
class WebDeploymentService implements Service<WebAppContext> {

    private static final Logger log = Logger.getLogger("org.jboss.web");
    private final WebAppContext context;
    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();

    public WebDeploymentService(final WebAppContext context) {
        this.context = context;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext startContext) throws StartException {
            try {
                webServer.getValue().addWebAppContext(context);
                context.start();
            } catch (Exception e) {
                throw new StartException("failed to start context", e);
            }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext stopContext) {
        try {
            context.stop();
            webServer.getValue().removeWebAppContext(context);
        } catch (Exception e) {
            log.error("exception while stopping context", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized WebAppContext getValue() throws IllegalStateException {
        final WebAppContext context = this.context;
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    InjectedValue<WebServer> getWebServer() {
        return webServer;
    }
}
