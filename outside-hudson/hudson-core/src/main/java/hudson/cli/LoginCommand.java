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
 *******************************************************************************/ 

package hudson.cli;

import hudson.Extension;
import hudson.model.Hudson;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.kohsuke.args4j.CmdLineException;
import org.springframework.security.core.Authentication;

/**
 * Saves the current credential to allow future commands to run without explicit
 * credential information.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.351
 */
@Extension
public class LoginCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Saves the current credential to allow future commands to run without explicit credential information";
    }

    /**
     * If we use the stored authentication for the login command, login becomes
     * no-op, which is clearly not what the user has intended.
     */
    @Override
    protected Authentication loadStoredAuthentication() throws InterruptedException {
        return HudsonSecurityManager.ANONYMOUS;
    }

    @Override
    protected int run() throws Exception {
        Authentication a = Hudson.getAuthentication();
        if (a == HudsonSecurityManager.ANONYMOUS) {
            throw new CmdLineException("No credentials specified."); // this causes CLI to show the command line options.
        }
        ClientAuthenticationCache store = new ClientAuthenticationCache(channel);
        store.set(a);

        return 0;
    }
}
