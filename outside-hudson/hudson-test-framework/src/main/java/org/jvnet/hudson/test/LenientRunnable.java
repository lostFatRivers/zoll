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

package org.jvnet.hudson.test;

/**
 * Like {@link Runnable} but can throw any exception.
 *
 * @author Kohsuke Kawaguchi
 */
public interface LenientRunnable {

    public void run() throws Exception;
}