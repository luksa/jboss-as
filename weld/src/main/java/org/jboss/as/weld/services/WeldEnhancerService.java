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

package org.jboss.as.weld.services;

import org.jboss.as.web.Enhancer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * Weld web enhancer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@SuppressWarnings({"unchecked"})
public class WeldEnhancerService implements Service<Enhancer>, Enhancer {

    private ServiceName serviceName;
    private final InjectedValue<BeanManager> beanManager = new InjectedValue<BeanManager>();

    public WeldEnhancerService(ServiceName serviceName) {
        this.serviceName = serviceName;
    }

    protected <T> T handle(Class<T> clazz, Object instance) {
        BeanManager manager = beanManager.getValue();
        InjectionTarget it = manager.createInjectionTarget(manager.createAnnotatedType(clazz));
        CreationalContext cc = manager.createCreationalContext(null);
        if (instance == null)
            instance = it.produce(cc);
        it.inject(instance, cc);
        return (T) instance;
    }

    @Override
    public <T> T create(Class<T> clazz) throws Exception {
        return clazz != null ? handle(clazz, null) : null;
    }

    @Override
    public <T> T enhance(T instance) {
        return (instance != null) ? (T) handle(instance.getClass(), instance) : null;
    }

    @Override
    public void destroy(Object instance) {
        if (instance != null) {
            BeanManager manager = beanManager.getValue();
            InjectionTarget it = manager.createInjectionTarget(manager.createAnnotatedType(instance.getClass()));
            it.dispose(instance);
        }
    }

    @Override
    public ServiceName getServiceName() {
        return serviceName;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public Enhancer getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<BeanManager> getBeanManager() {
        return beanManager;
    }
}
