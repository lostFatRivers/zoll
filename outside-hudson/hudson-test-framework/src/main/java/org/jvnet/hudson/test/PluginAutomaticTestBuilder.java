/*******************************************************************************
 *
 * Copyright (c) 2004-2009, Oracle Corporation
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

package org.jvnet.hudson.test;

import junit.framework.TestSuite;

import java.io.File;
import java.util.Map;

/**
 * Called by the code generated by maven-hpi-plugin to build tests for plugins.
 *
 * @author Kohsuke Kawaguchi
 */
public class PluginAutomaticTestBuilder {

    /**
     * @param params Various information about the plugin that maven-hpi-plugin
     * adds. As of 1.52, this includes the followings:
     *
     * basedir (String) : directory that contains pom.xml artifactId (String) :
     * artifact ID of the plugin outputDirectory (String) : target/classes dir
     * where class files and resources can be found testOutputDirectory (String)
     * : target/test-classes.
     */
    public static TestSuite build(Map<String, ?> params) throws Exception {
        TestSuite suite = new TestSuite();

        // automatic Jelly tests
        if (params.containsKey("outputDirectory") // shouldn't happen, but be defensive
                || notSkipTests("JellyTest")) {
            File outputDirectory = new File((String) params.get("outputDirectory"));
            suite.addTest(JellyTestSuiteBuilder.build(outputDirectory));
        }

        return suite;
    }

    /**
     * Provides an escape hatch for plugin developers to skip auto-generated
     * tests.
     */
    private static boolean notSkipTests(String propertyName) {
        return !Boolean.getBoolean("hpiTest.skip" + propertyName);
    }
}