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

import org.eclipse.jetty.util.resource.Resource;
import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS based Jetty Resource implementation.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class VFSResource extends Resource {

    private final VirtualFile file;
    private String[] list;

    VFSResource(VirtualFile file) {
        if (file == null)
            throw new IllegalArgumentException("Null file");
        this.file = file;
    }

    @Override
    public void release() {
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public long lastModified() {
        return file.getLastModified();
    }

    @Override
    public long length() {
        return file.getSize();
    }

    @Override
    public URL getURL() {
        try {
            return file.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public File getFile() throws IOException {
        return file.getPhysicalFile();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return file.openStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException, SecurityException {
        return new FileOutputStream(getFile());
    }

    @Override
    public boolean delete() throws SecurityException {
        return file.delete();
    }

    @Override
    public boolean renameTo(Resource dest) throws SecurityException {
        return false;
    }

    @Override
    public String[] list() {

        if (list == null) {
            List<String> files = new ArrayList<String>();
            for (VirtualFile child : file.getChildren()) {
                files.add(child.getName());
            }
            list = files.toArray(new String[files.size()]);
        }

        return list;
    }

    @Override
    public Resource addPath(String path) throws IOException {
        return new VFSResource(file.getChild(path));
    }

    @Override
    public boolean isContainedIn(Resource resource) throws MalformedURLException {
        if (resource instanceof VFSResource == false)
            return false;

        VFSResource vr = (VFSResource) resource;
        return file.getParentFileList().contains(vr.file);
    }
}
