/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Kohsuke Kawaguchi, Winston Prakash
 *
 *
 *******************************************************************************/ 

package hudson.cli;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.Hudson.MasterComputer;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.util.Secret;
import org.springframework.dao.DataAccessException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Properties;
import org.eclipse.hudson.jna.NativeUtils;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents the authentication credential store of the CLI client.
 *
 * <p> This object encapsulates a remote manipulation of the credential store.
 * We store encrypted user names.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.351
 */
public class ClientAuthenticationCache implements Serializable {

    /**
     * Where the store should be placed.
     */
    private final FilePath store;
    /**
     * Loaded contents of the store.
     */
    private final Properties props = new Properties();

    public ClientAuthenticationCache(Channel channel) throws IOException, InterruptedException {
        store = (channel == null ? MasterComputer.localChannel : channel).call(new Callable<FilePath, IOException>() {
            public FilePath call() throws IOException {
                File home = new File(System.getProperty("user.home"));
                return new FilePath(new File(home, ".hudson/cli-credentials"));
            }
        });
        if (store.exists()) {
            props.load(store.read());
        }
    }

    /**
     * Gets the persisted authentication for this Hudson.
     *
     * @return {@link Hudson#ANONYMOUS} if no such credential is found, or if
     * the stored credential is invalid.
     */
    public Authentication get() {
        Secret userName = Secret.decrypt(props.getProperty(getPropertyKey()));
        if (userName == null) {
            return Hudson.ANONYMOUS; // failed to decrypt
        }
        try {
            UserDetails u = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getSecurityRealm().loadUserByUsername(userName.toString());
            return new UsernamePasswordAuthenticationToken(u.getUsername(), u.getPassword(), u.getAuthorities());
        } catch (AuthenticationException e) {
            return Hudson.ANONYMOUS;
        } catch (DataAccessException e) {
            return Hudson.ANONYMOUS;
        }
    }

    /**
     * Computes the key that identifies this Hudson among other Hudsons that the
     * user has a credential for.
     */
    private String getPropertyKey() {
        String url = Hudson.getInstance().getRootUrl();
        if (url != null) {
            return url;
        }
        return Secret.fromString("key").toString();
    }

    /**
     * Persists the specified authentication.
     */
    public void set(Authentication a) throws IOException, InterruptedException {
        Hudson h = Hudson.getInstance();

        // make sure that this security realm is capable of retrieving the authentication by name,
        // as it's not required.
        UserDetails u = h.getSecurityRealm().loadUserByUsername(a.getName());
        props.setProperty(getPropertyKey(), Secret.fromString(u.getUsername()).getEncryptedValue());

        save();
    }

    /**
     * Removes the persisted credential, if there's one.
     */
    public void remove() throws IOException, InterruptedException {
        if (props.remove(getPropertyKey()) != null) {
            save();
        }
    }

    private void save() throws IOException, InterruptedException {
        final NativeUtils nativeUtils = NativeUtils.getInstance();
        store.act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                f.getParentFile().mkdirs();

                OutputStream os = new FileOutputStream(f);
                try {
                    props.store(os, "Credential store");
                } finally {
                    os.close();
                }

                // try to protect this file from other users, if we can.
                Util.chmod(f, 0600, false, null);
                return null;
            }
        });
    }
}
