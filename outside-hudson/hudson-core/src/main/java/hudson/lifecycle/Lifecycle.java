/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
*    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package hudson.lifecycle;

import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.Util;
import hudson.model.Hudson;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the capability for starting/stopping/restarting/uninstalling Hudson.
 *
 * <p> The steps to perform these operations depend on how Hudson is launched,
 * so the concrete instance of this method (which is VM-wide singleton) is
 * discovered by looking up a FQCN from the system property "hudson.lifecycle".
 *
 * @author Kohsuke Kawaguchi
 * @since 1.254
 */
public abstract class Lifecycle implements ExtensionPoint {

    private transient Logger logger = LoggerFactory.getLogger(Lifecycle.class);
    private static Lifecycle INSTANCE = null;

    /**
     * Gets the singleton instance.
     *
     * @return never null
     */
    public synchronized static Lifecycle get() {
        if (INSTANCE == null) {
            Lifecycle instance;
            String p = System.getProperty("hudson.lifecycle");
            // Do this first for better error reporting
            if (RestartCommandLifecycle.isConfigured())
                instance = new RestartCommandLifecycle();
            else if (p != null) {
                try {
                    ClassLoader cl = Hudson.getInstance().getPluginManager().uberClassLoader;
                    instance = (Lifecycle) cl.loadClass(p).newInstance();
                } catch (InstantiationException e) {
                    InstantiationError x = new InstantiationError(e.getMessage());
                    x.initCause(e);
                    throw x;
                } catch (IllegalAccessException e) {
                    IllegalAccessError x = new IllegalAccessError(e.getMessage());
                    x.initCause(e);
                    throw x;
                } catch (ClassNotFoundException e) {
                    NoClassDefFoundError x = new NoClassDefFoundError(e.getMessage());
                    x.initCause(e);
                    throw x;
                }
            } else {
                if (Functions.isWindows()) {
                    instance = new Lifecycle() {
                        @Override
                        public void verifyRestartable() throws RestartNotSupportedException {
                            throw new RestartNotSupportedException(
                                    "Default Windows lifecycle does not support restart.");
                        }
                    };
                } else if (System.getenv("SMF_FMRI") != null && System.getenv("SMF_RESTARTER") != null) {
                    // when we are run by Solaris SMF, these environment variables are set.
                    instance = new SolarisSMFLifecycle();
                } else {
                    instance = new UnixLifecycle();
                }
            }
            assert instance != null;
            INSTANCE = instance;
        }

        return INSTANCE;
    }

    /**
     * If the location of <tt>hudson.war</tt> is known in this life cycle,
     * return it location. Otherwise return null to indicate that it is unknown.
     *
     * <p> When a non-null value is returned, Hudson will offer an upgrade UI to
     * a newer version.
     */
    public File getHudsonWar() {
        String war = System.getProperty("executable-war");
        if (war != null && new File(war).exists()) {
            return new File(war);
        }
        return null;
    }

    /**
     * Replaces hudson.war by the given file.
     *
     * <p> On some system, most notably Windows, a file being in use cannot be
     * changed, so rewriting <tt>hudson.war</tt> requires some special trick.
     * Override this method to do so.
     */
    public void rewriteHudsonWar(File by) throws IOException {
        File dest = getHudsonWar();
        // this should be impossible given the canRewriteHudsonWar method,
        // but let's be defensive
        if (dest == null) {
            throw new IOException("hudson.war location is not known.");
        }

        // backing up the old hudson.war before it gets lost due to upgrading
        // (newly downloaded hudson.war and 'backup' (hudson.war.tmp) are the same files
        // unless we are trying to rewrite hudson.war by a backup itself
        File bak = new File(dest.getPath() + ".bak");
        if (!by.equals(bak)) {
            FileUtils.copyFile(dest, bak);
        }

        FileUtils.copyFile(by, dest);
        // we don't want to keep backup if we are downgrading
        if (by.equals(bak) && bak.exists()) {
            bak.delete();
        }
    }

    /**
     * Can {@link #rewriteHudsonWar(File)} work?
     */
    public boolean canRewriteHudsonWar() {
        // if we don't know where hudson.war is, it's impossible to replace.
        File f = getHudsonWar();
        return f != null && f.canWrite();
    }

    /**
     * If this life cycle supports a restart of Hudson, do so. Otherwise, throw
     * {@link UnsupportedOperationException}, which is what the default
     * implementation does.
     *
     * <p> The restart operation may happen synchronously (in which case this
     * method will never return), or asynchronously (in which case this method
     * will successfully return.)
     *
     * <p> Throw an exception if the operation fails unexpectedly.
     */
    public void restart() throws IOException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * Can the {@link #restart()} method restart Hudson?
     *
     * @throws RestartNotSupportedException If the restart is not supported,
     * throw this exception and explain the cause.
     */
    public void verifyRestartable() throws RestartNotSupportedException {
        // the rewriteHudsonWar method isn't overridden.
        if (!Util.isOverridden(Lifecycle.class, getClass(), "restart")) {
            throw new RestartNotSupportedException("Restart is not supported in this running mode ("
                    + getClass().getName() + ").");
        }
    }

    /**
     * The same as {@link #verifyRestartable()} except the status is indicated
     * by the return value, not by an exception.
     */
    public boolean canRestart() {
        try {
            verifyRestartable();
            return true;
        } catch (Throwable th) {
            // This could happen if the native libraries are not loaded properly.
            // Gracefully degrade rather than throwing exception
            logger.info(th.getLocalizedMessage());
            return false;
        }
    }
    
    /**
     * Return true if <code>restart</code> can be called in a safe restart.
     * Any lifecycle that is able to restart at all must be safe restartable,
     * but safe restartable lifecycles are not necessarily unsafe restartable
     * (without the shutdown sequence).
     * <p>
     * <code>isSafeRestartable</code> is appropriate for testing whether a
     * restart button or link should be shown when a configuration
     * option requires restart to take effect. The button must cause a
     * safe restart.
     * 
     * @since 3.0.1
     */
    public boolean isSafeRestartable() {
        return canRestart();
    }
}
