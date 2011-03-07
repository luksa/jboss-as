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
package org.jboss.as.web;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Service configuring and starting the web container.
 *
 * @author Emanuel Muckenhuber
 * @author Ales Justin
 */
class WebServerService implements WebServer, Service<WebServer> {

    private Server server;
    private ContextHandlerCollection contexts;

    private final InjectedValue<Integer> portInjector = new InjectedValue<Integer>();

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        Integer port = portInjector.getOptionalValue();
        if (port == null)
            port = 8080;
        server = new Server(port);

        HandlerCollection handlers = new HandlerCollection();
        contexts = new ContextHandlerCollection();
        handlers.addHandler(contexts);
        handlers.addHandler(new DefaultHandler());
        // handlers.addHandler(new RequestLogHandler());
        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        try {
            server.stop();
        } catch (Exception ignored) {
        }
    }

    /** {@inheritDoc} */
    public synchronized WebServer getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    public synchronized void addConnector(Connector connector) {
        server.addConnector(connector);
    }

    /** {@inheritDoc} */
    public synchronized void removeConnector(Connector connector) {
        server.removeConnector(connector);
    }

    @Override
    public void addWebAppContext(WebAppContext context) {
        try {
            contexts.addHandler(context);
            contexts.mapContexts();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void removeWebAppContext(WebAppContext context) {
        contexts.removeHandler(context);
    }

    InjectedValue<Integer> getPortInjector() {
        return portInjector;
    }
}
