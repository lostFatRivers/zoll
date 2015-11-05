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
 *    Kohsuke Kawaguchi
 *
 *******************************************************************************/ 

package hudson.security;

import hudson.model.Executor;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Gate-keeper that controls access to Hudson's model objects.
 *
 * @author Kohsuke Kawaguchi
 * @see
 * http://wiki.hudson-ci.org/display/HUDSON/Making+your+plugin+behave+in+secured+Hudson
 */
public abstract class ACL {

    /**
     * Checks if the current security principal has this permission.
     *
     * <p> This is just a convenience function.
     *
     * @throws org.acegisecurity.AccessDeniedException if the user doesn't have
     * the permission.
     */
    public final void checkPermission(Permission p) {
        Authentication a = HudsonSecurityManager.getAuthentication();
        if (!hasPermission(a, p)) {
            throw new AccessDeniedException2(a, p);
        }
    }

    /**
     * Checks if the current security principal has this permission.
     *
     * @return false if the user doesn't have the permission.
     */
    public final boolean hasPermission(Permission p) {
        return hasPermission(HudsonSecurityManager.getAuthentication(), p);
    }

    /**
     * Checks if the given principle has the given permission.
     *
     * <p> Note that {@link #SYSTEM} can be passed in as the authentication
     * parameter, in which case you should probably just assume it has every
     * permission.
     */
    public abstract boolean hasPermission(Authentication a, Permission permission);
    //
    // Sid constants
    //
    /**
     * Special {@link Sid} that represents "everyone", even including anonymous
     * users.
     *
     * <p> This doesn't need to be included in
     * {@link Authentication#getAuthorities()}, but {@link ACL} is responsible
     * for checking it nontheless, as if it was the last entry in the granted
     * authority.
     */
    public static final Sid EVERYONE = new Sid() {
        @Override
        public String toString() {
            return "EVERYONE";
        }
    };
    /**
     * {@link Sid} that represents the anonymous unauthenticated users. <p>
     * {@link HudsonFilter} sets this up, so this sid remains the same
     * regardless of the current {@link SecurityRealm} in use.
     */
    public static final Sid ANONYMOUS = new PrincipalSid("anonymous");
    protected static final Sid[] AUTOMATIC_SIDS = new Sid[]{EVERYONE, ANONYMOUS};
    public static final List<GrantedAuthority> NO_AUTHORITIES = new ArrayList<GrantedAuthority>();
     
    /**
     * {@link Sid} that represents the Hudson itself. <p> This is used when
     * Hudson is performing computation for itself, instead of acting on behalf
     * of an user, such as doing builds.
     *
     * <p> (Note that one of the features being considered is to keep track of
     * who triggered a build &mdash; so in a future, perhaps {@link Executor}
     * will run on behalf of the user who triggered a build.)
     */
    public static final Authentication SYSTEM = new UsernamePasswordAuthenticationToken("SYSTEM", "SYSTEM", NO_AUTHORITIES);
}
