/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.persistenceprovider;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Implementation of PersistenceProviderResolver
 * TODO:  look at forking/merging with Hibernate javax.persistence.spi.PersistenceProviderResolverHolder.PersistenceProviderResolverPerClassLoader
 * TODO -- this needs a better impl, as per-app-bundled JPA doesn't really properly work
 *
 * @author Scott Marlow
 * @author Ales Justin
 */
public class PersistenceProviderResolverImpl implements PersistenceProviderResolver {

    private Set<PersistenceProvider> providers = new CopyOnWriteArraySet<PersistenceProvider>();

    private static final PersistenceProviderResolverImpl INSTANCE = new PersistenceProviderResolverImpl();

    public static PersistenceProviderResolverImpl getInstance() {
        return INSTANCE;
    }

    private PersistenceProviderResolverImpl() {
    }

    /**
     * Return a copy of providers set.
     *
     * @return new providers list
     */
    public List<PersistenceProvider> getPersistenceProviders() {
        return new ArrayList<PersistenceProvider>(providers);
    }

    public void clearCachedProviders() {
        providers.clear();
    }

    public void addPersistenceProvider(PersistenceProvider persistenceProvider) {
        providers.add(persistenceProvider);
    }

    public void removePersistenceProvider(PersistenceProvider persistenceProvider) {
        providers.remove(persistenceProvider);
    }

}
