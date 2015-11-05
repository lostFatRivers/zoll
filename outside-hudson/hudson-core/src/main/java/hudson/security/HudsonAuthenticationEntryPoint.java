/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi
 *
 *
 ******************************************************************************
 */
package hudson.security;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.MessageFormat;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * For anonymous requests to pages that require authentication, first respond
 * with {@link HttpServletResponse#SC_FORBIDDEN}, then redirect browsers
 * automatically to the login page.
 *
 * <p>
 * This is a compromise to handle programmatic access and real browsers equally
 * well.
 *
 * <p>
 * The page that programs see is entirely white, and it auto-redirects, so
 * humans wouldn't notice it.
 *
 * @author Kohsuke Kawaguchi
 */
public class HudsonAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rsp = (HttpServletResponse) response;

        String requestedWith = req.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(requestedWith)) {
            // container authentication normally relies on session attribute to
            // remember where the user came from, so concurrent AJAX requests
            // often ends up sending users back to AJAX pages after successful login.
            // this is not desirable, so don't redirect AJAX requests to the user.
            // this header value is sent from Prototype.
            rsp.sendError(SC_FORBIDDEN);
        } else {
            // give the opportunity to include the target URL
            String loginForm = req.getContextPath() + getLoginFormUrl();
            loginForm = MessageFormat.format(loginForm, URLEncoder.encode(req.getRequestURI(), "UTF-8"));
            req.setAttribute("loginForm", loginForm);

            rsp.setStatus(SC_FORBIDDEN);
            rsp.setContentType("text/html;charset=UTF-8");
            PrintWriter out;
            try {
                ServletOutputStream sout = rsp.getOutputStream();
                out = new PrintWriter(new OutputStreamWriter(sout));
            } catch (IllegalStateException e) {
                out = rsp.getWriter();
            }
            out.printf(
                    "<html><head>"
                    + "<meta http-equiv='refresh' content='1;url=%1$s'/>"
                    + "<script>window.location.replace('%1$s');</script>"
                    + "</head>"
                    + "<body style='background-color:white; color:white;'>"
                    + "Authentication required</body></html>", loginForm);
            // Turn Off "Show Friendly HTTP Error Messages" Feature on the Server Side.
            // See http://support.microsoft.com/kb/294807
            for (int i = 0; i < 10; i++) {
                out.print("                              ");
            }
            out.flush();
        }
    }
}
