/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/

package org.eclipse.hudson.model.project.property;

import hudson.model.FreeStyleProjectMock;
import hudson.tasks.LogRotator;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Contains test-cases for IProjectProperty and its' implementations.
 * <p/>
 * Date: 9/27/11
 *
 * @author Nikita Levyankov
 */
@SuppressWarnings("unchecked")
public class ProjectPropertyTest {
    private FreeStyleProjectMock project;
    private final String propertyKey = "propertyKey";

    @Before
    public void setUp() {
        project = new FreeStyleProjectMock("project");
    }

    @Test
    public void testStringProjectConstructor() {
        try {
            new StringProjectProperty(null);
            fail("Null should be handled by StringProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    @Test
    public void testIntegerProjectConstructor() {
        try {
            new IntegerProjectProperty(null);
            fail("Null should be handled by IntegerProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    @Test
    public void testBooleanProjectPropertyConstructor() {
        try {
            new BooleanProjectProperty(null);
            fail("Null should be handled by BooleanProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    @Test
    public void testLogRotatorProjectPropertyConstructor() {
        try {
            new LogRotatorProjectProperty(null);
            fail("Null should be handled by LogRotatorProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    @Test
    public void testBooleanProjectPropertyPrepareValue() {
        //Boolean property acts as BaseProperty
        BaseProjectProperty property = new BooleanProjectProperty(project);
        assertNull(property.prepareValue(null));
        assertFalse((Boolean) property.prepareValue(false));
        assertTrue((Boolean) property.prepareValue(true));
    }

    @Test
    public void testLogRotatorProjectPropertyPrepareValue() {
        //Boolean property acts as BaseProperty
        BaseProjectProperty property = new LogRotatorProjectProperty(project);
        assertNull(property.prepareValue(null));
        LogRotator value = new LogRotator(1, 1, 1, 1);
        assertEquals(value, property.prepareValue(value));
    }

    @Test
    public void testIntegerProjectPropertyPrepareValue() {
        //Integer property acts as BaseProperty
        BaseProjectProperty property = new IntegerProjectProperty(project);
        assertNull(property.prepareValue(null));
        int intValue = 10;
        assertEquals(intValue, property.prepareValue(intValue));
    }

    @Test
    public void testStringProjectPropertyPrepareValue() {
        //String project property trims string values to null and uses StringUtils.trimToNull logic.
        BaseProjectProperty property = new StringProjectProperty(project);
        assertNull(property.prepareValue(null));
        assertNull(property.prepareValue(""));
        assertNull(property.prepareValue("     "));
        assertEquals("abc", property.prepareValue("abc"));
        assertEquals("abc", property.prepareValue("    abc    "));
    }

    @Test
    public void testStringProjectPropertyGetDefaultValue() {
        BaseProjectProperty property = new StringProjectProperty(project);
        assertNull(property.getDefaultValue());
    }

    @Test
    public void testIntegerProjectPropertyGetDefaultValue() {
        BaseProjectProperty property = new IntegerProjectProperty(project);
        assertEquals(0, property.getDefaultValue());
    }

    @Test
    public void testBooleanProjectPropertyGetDefaultValue() {
        BaseProjectProperty property = new BooleanProjectProperty(project);
        assertFalse((Boolean) property.getDefaultValue());
    }

    @Test
    public void testLogRotatorProjectPropertyGetDefaultValue() {
        BaseProjectProperty property = new LogRotatorProjectProperty(project);
        assertNull(property.getDefaultValue());
    }

    @Test
    public void testBooleanProjectPropertyAllowOverrideValue() {
        BaseProjectProperty property = new BooleanProjectProperty(project);
        assertFalse(property.allowOverrideValue(null, null));
        assertFalse(property.allowOverrideValue(false, false));
        assertFalse(property.allowOverrideValue(true, true));
        assertTrue(property.allowOverrideValue(true, false));
        assertTrue(property.allowOverrideValue(false, true));
    }

    @Test
    public void testIntegerProjectPropertyAllowOverrideValue() {
        BaseProjectProperty property = new IntegerProjectProperty(project);
        assertFalse(property.allowOverrideValue(null, null));
        assertFalse(property.allowOverrideValue(1, 1));
        assertTrue(property.allowOverrideValue(1, 0));
        assertTrue(property.allowOverrideValue(0, 1));
    }

    @Test
    public void testStringProjectPropertyAllowOverrideValue() {
        BaseProjectProperty property = new StringProjectProperty(project);
        assertFalse(property.allowOverrideValue(null, null));
        assertFalse(property.allowOverrideValue("", ""));
        assertFalse(property.allowOverrideValue("abc", "abc"));
        assertFalse(property.allowOverrideValue("abc", "ABC"));
        assertTrue(property.allowOverrideValue(null, "abc"));
        assertTrue(property.allowOverrideValue("abc", null));
        assertTrue(property.allowOverrideValue("abc", "abcd"));
    }

    @Test
    public void testLogRotatorProjectPropertyAllowOverrideValue() {
        BaseProjectProperty property = new LogRotatorProjectProperty(project);
        assertFalse(property.allowOverrideValue(null, null));
        assertTrue(property.allowOverrideValue(new LogRotator(1, 1, 1, 1), null));
        assertTrue(property.allowOverrideValue(null, new LogRotator(1, 1, 1, 1)));
        assertTrue(property.allowOverrideValue(new LogRotator(1, 1, 1, 2), new LogRotator(1, 1, 1, 1)));
    }

    @Test
    public void testIntegerProjectPropertyGetOriginalValue() {
        int value = 10;
        BaseProjectProperty property = new IntegerProjectProperty(project);
        property.setKey(propertyKey);
        property.setValue(value);
        assertEquals(value, property.getOriginalValue());
    }

    @Test
    public void testStringProjectPropertyGetOriginalValue() {
        String value = "abs";
        BaseProjectProperty property = new StringProjectProperty(project);
        property.setKey(propertyKey);
        property.setValue(value);
        assertEquals(value, property.getOriginalValue());
    }

    @Test
    public void testBooleanProjectPropertyGetOriginalValue() {
        boolean value = Boolean.TRUE;
        BaseProjectProperty property = new BooleanProjectProperty(project);
        property.setKey(propertyKey);
        property.setValue(value);
        assertEquals(value, property.getOriginalValue());
        property.setValue(null);
        assertFalse((Boolean) property.getOriginalValue());
    }

    @Test
    public void testLogRotatorProjectPropertyGetOriginalValue() {
        LogRotator value = new LogRotator(1, 1, 1, 1);
        BaseProjectProperty property = new LogRotatorProjectProperty(project);
        property.setKey(propertyKey);
        property.setValue(value);
        assertEquals(value, property.getOriginalValue());
    }
}
