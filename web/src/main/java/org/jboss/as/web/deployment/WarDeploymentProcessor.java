/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.JettyWebXmlConfiguration;
import org.mortbay.jetty.webapp.WebAppContext;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Emanuel Muckenhuber
 * @author Anil.Saldhana@redhat.com
 */
public class WarDeploymentProcessor implements DeploymentUnitProcessor {

    private final String defaultHost;

    public WarDeploymentProcessor(String defaultHost) {
        if (defaultHost == null) {
            throw new IllegalArgumentException("null default host");
        }
        this.defaultHost = defaultHost;
    }

    /** {@inheritDoc} */
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (metaData == null) {
            return;
        }
        Collection<String> hostNames = metaData.getMergedJBossWebMetaData().getVirtualHosts();
        if (hostNames == null || hostNames.isEmpty()) {
            hostNames = Collections.singleton(defaultHost);
        }
        String hostName = hostNames.iterator().next();
        // FIXME: Support automagic aliases ?
        if (hostName == null) {
            throw new IllegalStateException("null host name");
        }
        processDeployment(hostName, metaData, deploymentUnit, phaseContext.getServiceTarget());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    protected void processDeployment(final String hostName, final WarMetaData warMetaData, final DeploymentUnit deploymentUnit,
            final ServiceTarget serviceTarget) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for deployment " + deploymentRoot);
        }
        final ClassLoader classLoader = module.getClassLoader();
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();

        // Create the context
        final WebAppContext webContext = new WebAppContext();
        webContext.setBaseResource(new VFSResource(deploymentRoot));
        webContext.setWar(deploymentRoot.getPathName());
        webContext.setExtractWAR(false);
        webContext.setClassLoader(classLoader);

        applyConfigurations(metaData, webContext);

        // Set the path name
        final String deploymentName = deploymentUnit.getName();
        String pathName;
        if (metaData.getContextRoot() == null) {
            pathName = deploymentRoot.getName();
            if (pathName.equals("ROOT.war")) {
                pathName = "";
            } else {
                pathName = "/" + pathName.substring(0, pathName.length() - 4);
            }
        } else {
            pathName = metaData.getContextRoot();
            if ("/".equals(pathName)) {
                pathName = "";
            }
        }
        webContext.setContextPath(pathName);

        try {
            WebDeploymentService webDeploymentService = new WebDeploymentService(webContext);
            serviceTarget.addService(WebSubsystemServices.JBOSS_WEB.append(deploymentName), webDeploymentService)
                    .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, webDeploymentService.getWebServer())
                    .setInitialMode(Mode.ACTIVE).install();
        } catch (ServiceRegistryException e) {
            throw new DeploymentUnitProcessingException("Failed to add JBoss web deployment service", e);
        }
    }

    protected void applyConfigurations(JBossWebMetaData metaData, WebAppContext webContext) {
        JBossWebConfiguration jwc = new JBossWebConfiguration(metaData);
        jwc.setWebAppContext(webContext);
        JettyWebXmlConfiguration jettyWebXmlConfiguration = new JettyWebXmlConfiguration();
        jettyWebXmlConfiguration.setWebAppContext(webContext);
        webContext.setConfigurations(new Configuration[]{jwc, jettyWebXmlConfiguration});
    }
}
