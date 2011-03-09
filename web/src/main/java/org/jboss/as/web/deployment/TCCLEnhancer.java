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

import org.jboss.as.web.Enhancer;
import org.jboss.msc.service.ServiceName;

/**
 * Wraps TCCL usage around enhancer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class TCCLEnhancer implements Enhancer {

    private final ClassLoader cl;
    private final Enhancer delegate;

    TCCLEnhancer(ClassLoader cl, Enhancer delegate) {
        this.cl = cl;
        this.delegate = delegate;
    }

    @Override
    public <T> T create(Class<T> clazz) throws Exception {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return delegate.create(clazz);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public <T> T enhance(T instance) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return delegate.enhance(instance);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public void destroy(Object instance) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            delegate.destroy(instance);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public ServiceName getServiceName() {
        return delegate.getServiceName();
    }
}
