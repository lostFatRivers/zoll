/*******************************************************************************
 *
 * Copyright (c) 2011-2012, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *  Kohsuke Kawaguchi, Nikita Levyankov, Winston Prakash
 *
 *******************************************************************************/ 

package hudson.security;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.cli.CLICommand;
import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.tasks.MailAddressResolver;
import java.io.Console;
import java.io.IOException;
import java.util.Arrays;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.kohsuke.args4j.Option;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Partial implementation of {@link SecurityRealm} for username/password based
 * authentication. This is a convenience base class if all you are trying to do
 * is to check the given username and password with the information stored in
 * somewhere else, and you don't want to do anything with Spring Security.
 * <p/>
 * <
 * p/>
 * This {@link SecurityRealm} uses the standard login form (and a few other
 * optional mechanisms like BASIC auth) to gather the username/password
 * information. Subtypes are responsible for authenticating this information.
 *
 * @author Kohsuke Kawaguchi
 * @author Nikita Levyankov
 * @since 1.317
 */
public abstract class AbstractPasswordBasedSecurityRealm extends SecurityRealm implements UserDetailsService {

    @Override
    public SecurityComponents createSecurityComponents() {

        // this does all the hard work 
        Authenticator authenticator = new Authenticator();

        // these providers apply everywhere
        RememberMeAuthenticationProvider rememberMeAuthenticationProvider = new RememberMeAuthenticationProvider();
        rememberMeAuthenticationProvider.setKey(HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getSecretKey());

        // this doesn't mean we allow anonymous access.
        // we just authenticate anonymous users as such,
        // so that later authorization can reject them if so configured
        AnonymousAuthenticationProvider anonymousAuthenticationProvider = new AnonymousAuthenticationProvider();
        anonymousAuthenticationProvider.setKey("anonymous");

        AuthenticationProvider[] authenticationProvider = {
            authenticator,
            rememberMeAuthenticationProvider,
            anonymousAuthenticationProvider
        };

        ProviderManager providerManager = new ProviderManager();
        providerManager.setProviders(Arrays.asList(authenticationProvider));
        return new SecurityComponents(providerManager, this);

    }

    @Override
    public CliAuthenticator createCliAuthenticator(final CLICommand command) {
        return new CliAuthenticator() {
            @Option(name = "--username", usage = "User name to authenticate yourself to Hudson")
            public String userName;
            @Option(name = "--password",
            usage = "Password for authentication. Note that passing a password in arguments is insecure.")
            public String password;
            @Option(name = "--password-file", usage = "File that contains the password")
            public String passwordFile;

            public Authentication authenticate() throws AuthenticationException, IOException, InterruptedException {
                if (userName == null) {
                    return Hudson.ANONYMOUS;    // no authentication parameter. run as anonymous
                }

                if (passwordFile != null) {
                    try {
                        password = new FilePath(command.channel, passwordFile).readToString().trim();
                    } catch (IOException e) {
                        throw new BadCredentialsException("Failed to read " + passwordFile, e);
                    }
                }
                if (password == null) {
                    password = command.channel.call(new InteractivelyAskForPassword());
                }

                if (password == null) {
                    throw new BadCredentialsException("No password specified");
                }

                UserDetails d = AbstractPasswordBasedSecurityRealm.this.doAuthenticate(userName, password);
                return new UsernamePasswordAuthenticationToken(d, password, d.getAuthorities());
            }
        };
    }

    /**
     * Authenticate a login attempt. This method is the heart of a
     * {@link AbstractPasswordBasedSecurityRealm}.
     * <p/>
     * <
     * p/>
     * If the user name and the password pair matches, retrieve the information
     * about this user and return it as a {@link UserDetails} object.
     * {@link org.springframework.security.userdetails.User} is a convenient
     * implementation to use, but if your backend offers additional data, you
     * may want to use your own subtype so that the rest of Hudson can use those
     * additional information (such as e-mail address --- see
     * {@link MailAddressResolver}.)
     * <p/>
     * <
     * p/>
     * Properties like {@link UserDetails#getPassword()} make no sense, so just
     * return an empty value from it. The only information that you need to pay
     * real attention is {@link UserDetails#getAuthorities()}, which is a list
     * of roles/groups that the user is in. At minimum, this must contain
     * {@link #AUTHENTICATED_AUTHORITY} (which indicates that this user is
     * authenticated and not anonymous), but if your backend supports a notion
     * of groups, you should make sure that the authorities contain one entry
     * per one group. This enables users to control authorization based on
     * groups.
     * <p/>
     * <
     * p/>
     * If the user name and the password pair doesn't match, throw
     * {@link AuthenticationException} to reject the login attempt. If
     * authentication was successful - HUDSON_USER environment variable will be
     * set <a
     * href='http://issues.hudson-ci.org/browse/HUDSON-4463'>HUDSON-4463</a>
     */
    protected UserDetails doAuthenticate(String username, String password) throws AuthenticationException {
        UserDetails userDetails = authenticate(username, password);
        EnvVars.setHudsonUserEnvVar(userDetails.getUsername());
        return userDetails;
    }

    /**
     * Implements same logic as {@link #doAuthenticate(String, String)} method,
     * but doesn't set hudson_user env variable.
     */
    protected abstract UserDetails authenticate(String username, String password) throws AuthenticationException;

    /**
     * Retrieves information about an user by its name.
     * <p/>
     * <
     * p/>
     * This method is used, for example, to validate if the given token is a
     * valid user name when the user is configuring an ACL. This is an optional
     * method that improves the user experience. If your backend doesn't support
     * a query like this, just always throw {@link UsernameNotFoundException}.
     */
    @Override
    public abstract UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException;

    /**
     * Retrieves information about a group by its name.
     * <p/>
     * This method is the group version of the
     * {@link #loadUserByUsername(String)}.
     */
    @Override
    public abstract GroupDetails loadGroupByGroupname(String groupname)
            throws UsernameNotFoundException, DataAccessException;

    class Authenticator extends AbstractUserDetailsAuthenticationProvider {

        protected void additionalAuthenticationChecks(UserDetails userDetails,
                UsernamePasswordAuthenticationToken authentication)
                throws AuthenticationException {
            // authentication is assumed to be done already in the retrieveUser method
        }

        protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
                throws AuthenticationException {
            return AbstractPasswordBasedSecurityRealm.this.doAuthenticate(username,
                    authentication.getCredentials().toString());
        }
    }

    /**
     * Asks for the password.
     */
    private static class InteractivelyAskForPassword implements Callable<String, IOException> {

        public String call() throws IOException {
            Console console = System.console();
            if (console == null) {
                return null;    // no terminal
            }

            char[] w = console.readPassword("Password:");
            if (w == null) {
                return null;
            }
            return new String(w);
        }
        private static final long serialVersionUID = 1L;
    }
}
