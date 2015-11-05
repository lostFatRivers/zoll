/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *******************************************************************************/ 

package hudson.init;

import org.jvnet.hudson.reactor.ReactorListener;
import hudson.model.Hudson;

/**
 * {@link ReactorListener}s that get notified of the Hudson initialization
 * process.
 *
 * <p> Because the act of initializing plugins is a part of the Hudson
 * initialization, this extension point cannot be implemented in a plugin. You
 * need to place your jar inside {@code WEB-INF/lib} instead.
 *
 * <p> Register your implementation at META-INF/services.
 *
 * @author Kohsuke Kawaguchi
 * @see Hudson#buildReactorListener()
 */
public interface InitReactorListener extends ReactorListener {
}
