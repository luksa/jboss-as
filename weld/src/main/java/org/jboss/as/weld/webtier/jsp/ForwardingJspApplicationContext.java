/*
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

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;

/**
 * @author pmuir
 * @author alesj
 */
public abstract class ForwardingJspApplicationContext extends JspApplicationContextImpl {

    protected ForwardingJspApplicationContext(ServletContext context) {
        super(context);
    }

    protected abstract JspApplicationContext delegate();

    @Override
    public void addELContextListener(ELContextListener listener) {
        delegate().addELContextListener(listener);
    }

    @Override
    public void addELResolver(ELResolver resolver) throws IllegalStateException {
        delegate().addELResolver(resolver);
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return delegate().getExpressionFactory();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

}