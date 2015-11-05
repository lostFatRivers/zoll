/*******************************************************************************
 *
 * Copyright (c) 2004-2010, Oracle Corporation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *
 *
 *******************************************************************************/ 

package hudson.security;

import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Handles authentication for CLI commands.
 *
 * <p> {@link CliAuthenticator} is used to authenticate an invocation of the CLI
 * command, so that the thread carries the correct {@link Authentication} that
 * represents the user who's invoking the command.
 *
 * <h2>Lifecycle</h2> <p> Each time a CLI command is invoked,
 * {@link SecurityRealm#createCliAuthenticator(CLICommand)} is called to
 * allocate a fresh {@link CliAuthenticator} object.
 *
 * <p> The {@link Option} and {@link Argument} annotations on the returned
 * {@link CliAuthenticator} instance are scanned and added into the
 * {@link CmdLineParser}, then that parser is used to parse command line
 * arguments. This means subtypes can define fields/setters with those
 * annotations to define authentication-specific options to CLI commands.
 *
 * <p> Once the arguments and options are parsed and populated,
 * {@link #authenticate()} method is called to perform the authentications. If
 * the authentication succeeds, this method returns an {@link Authentication}
 * instance that represents the user. If the authentication fails, this method
 * throws {@link AuthenticationException}. To authenticate, the method can use
 * parsed argument/option values, as well as interacting with the client via
 * {@link CLICommand} by using its stdin/stdout and its channel (for example, if
 * you want to interactively prompt a password, you can do so by using
 * {@link CLICommand#channel}.)
 *
 * <p> If no explicit credential is provided, or if the {@link SecurityRealm}
 * depends on a mode of authentication that doesn't involve in explicit password
 * (such as Kerberos), it's also often useful to fall back to
 * {@link CLICommand#getTransportAuthentication()}, in case the user is
 * authenticated at the transport level.
 *
 * <p> Many commands do not require any authentication (for example, the "help"
 * command), and still more commands can be run successfully with the anonymous
 * permission. So the authenticator should normally allow unauthenticated CLI
 * command invocations. For those, return {@link Hudson#ANONYMOUS} from the
 * {@link #authenticate()} method.
 *
 * <h2>Example</h2> <p> For a complete example, see the implementation of
 * {@link AbstractPasswordBasedSecurityRealm#createCliAuthenticator(CLICommand)}
 *
 * @author Kohsuke Kawaguchi
 * @since 1.350
 */
public abstract class CliAuthenticator {

    /**
     * Authenticates the CLI invocation. See class javadoc for the semantics.
     *
     * @throws AuthenticationException If the authentication failed and hence
     * the processing shouldn't proceed.
     * @throws IOException Can be thrown if the {@link CliAuthenticator} fails
     * to interact with the client. This exception is treated as a failure of
     * authentication. It's just that allowing this would often simplify the
     * callee.
     * @throws InterruptedException Same motivation as {@link IOException}.
     * Treated as an authentication failure.
     */
    public abstract Authentication authenticate() throws AuthenticationException, IOException, InterruptedException;
}
