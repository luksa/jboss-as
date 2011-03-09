/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.webtier.jsp;

import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.weld.util.Reflections;
import org.jboss.weld.servlet.api.helpers.AbstractServletListener;

import javax.el.ELContextListener;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * The Web Beans JSP initialization listener
 *
 *
 * @author Pete Muir
 * @author Stuart Douglas
 * @author Ales Justin
 */
public class JspInitializationListener extends AbstractServletListener {

    private volatile boolean installed = false;
    private static final String EXPRESSION_FACTORY_NAME = "org.jboss.weld.el.ExpressionFactory";
    private static Map<ServletContext, JspApplicationContextImpl> hackMap;

    static {
        try {
            Field field = JspApplicationContextImpl.class.getDeclaredField("map");
            field.setAccessible(true);
            //noinspection unchecked
            hackMap = (Map<ServletContext, JspApplicationContextImpl>) field.get(null);
        } catch (Throwable t) {
            System.err.println("Cannot hack JSP Jasper: " + t.getMessage());
        }
    }

    @Inject
    private BeanManager beanManager;

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        if (!installed && beanManager != null && JspFactory.getDefaultFactory() != null) {
            synchronized (this) {
                if (!installed) {
                    installed = true;
                    ServletContext context = sre.getServletContext();

                    // get JspApplicationContext.
                    JspApplicationContext jspAppContext = JspFactory.getDefaultFactory().getJspApplicationContext(context);

                    // register compositeELResolver with JSP
                    jspAppContext.addELResolver(beanManager.getELResolver());
                    jspAppContext.addELContextListener(Reflections.<ELContextListener>newInstance("org.jboss.weld.el.WeldELContextListener", getClass().getClassLoader()));

                    // Hack into Jasper to replace the ExpressionFactory
                    ExpressionFactory weldEF = beanManager.wrapExpressionFactory(jspAppContext.getExpressionFactory());
                    JspApplicationContextImpl wrappedJspApplicationContextImpl = new WeldJspApplicationContextImpl(context, jspAppContext, weldEF);
                    if (hackMap != null)
                        hackMap.put(context, wrappedJspApplicationContextImpl);

                     // Push the wrapped expression factory into the servlet context so that Tomcat or Jetty can hook it in using a container code
                    context.setAttribute(EXPRESSION_FACTORY_NAME, weldEF);
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        JspApplicationContextImpl.removeJspApplicationContext(sce.getServletContext());
    }

    private static class WeldJspApplicationContextImpl extends ForwardingJspApplicationContext {
        private final JspApplicationContext delegate;
        private final ExpressionFactory expressionFactory;

        public WeldJspApplicationContextImpl(ServletContext context, JspApplicationContext delegate, ExpressionFactory expressionFactory) {
            super(context);
            this.delegate = delegate;
            this.expressionFactory = expressionFactory;
        }

        @Override
        protected JspApplicationContext delegate() {
            return delegate;
        }

        @Override
        public ExpressionFactory getExpressionFactory() {
            return expressionFactory;
        }
    }
}
