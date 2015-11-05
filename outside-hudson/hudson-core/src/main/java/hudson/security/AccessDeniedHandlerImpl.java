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

import hudson.model.Hudson;
import org.kohsuke.stapler.Stapler;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Handles {@link AccessDeniedException} happened during request processing.
 * Specifically, send 403 error code and the login page.
 *
 * @author Kohsuke Kawaguchi
 */
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rsp = (HttpServletResponse) response;

        rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute("exception", accessDeniedException);
        Stapler stapler = new Stapler();
        stapler.init(new ServletConfig() {
            public String getServletName() {
                return "Stapler";
            }

            public ServletContext getServletContext() {
                return Hudson.getInstance().servletContext;
            }

            public String getInitParameter(String name) {
                return null;
            }

            public Enumeration getInitParameterNames() {
                return new Vector().elements();
            }
        });

        stapler.invoke(req, rsp, Hudson.getInstance(), "/accessDenied");
    } 
}
