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

package hudson.security;


import javax.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link UserDetails} that can mark {@link Authentication} invalid.
 *
 * <p> Tomcat persists sessions by using Java serialization (and that includes
 * the security token created by Spring Security, which includes this object)
 * and when that happens, the next time the server comes back it will try to
 * deserialize {@link SecurityContext} that Spring Security puts into
 * {@link HttpSession} (which transitively includes {@link UserDetails} that can
 * be implemented by Hudson.
 *
 * <p> Such {@link UserDetails} implementation can override the
 * {@link #isInvalid()} method and return false, so that such
 * {@link SecurityContext} will be dropped before the rest of Spring Security
 * sees it.
 *
 * <p> See http://issues.hudson-ci.org/browse/HUDSON-1482
 *
 * @author Kohsuke Kawaguchi
 * @deprecated Starting 1.285, Hudson stops persisting {@link Authentication}
 * altogether (see {@link NotSerilizableSecurityContext}), so there's no need to
 * use this mechanism.
 */
public interface InvalidatableUserDetails extends UserDetails {

    boolean isInvalid();
}
