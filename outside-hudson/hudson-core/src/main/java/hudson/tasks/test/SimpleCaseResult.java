/*******************************************************************************
 *
 * Copyright (c) 2009, Yahoo!, Inc.
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

package hudson.tasks.test;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.TestAction;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;

/**
 * The simplest possible case result, with no language ties. Acts as if it
 * passed, has no children, and has no failed or skipped tests.
 */
public class SimpleCaseResult extends TestResult {

    protected AbstractTestResultAction parentAction;
    protected final List<SimpleCaseResult> listOnlyContainingThisObject = new ArrayList<SimpleCaseResult>(1);
    protected float duration = 1.0f;
    private static final Logger LOGGER = Logger.getLogger(SimpleCaseResult.class.getName());

    public SimpleCaseResult(float duration) {
        listOnlyContainingThisObject.add(this);
    }

    public SimpleCaseResult() {
        this(1.0f);
    }

    /**
     * Sets the parent action, which means the action that binds this particular
     * case result to a build. Should not be null.
     *
     * @param parentAction
     */
    @Override
    public void setParentAction(AbstractTestResultAction parentAction) {
        this.parentAction = parentAction;
    }

    @Override
    public AbstractTestResultAction getParentAction() {
        return this.parentAction;
    }

    @Override
    public TestObject getParent() {
        return null;
    }

    @Override
    public TestResult findCorrespondingResult(String id) {
        if (id.equals(getId())) {
            return this;
        }

        return null;
    }

    /**
     * Gets the "children" of this test result that failed
     *
     * @return the children of this test result, if any, or an empty collection
     */
    @Override
    public Collection<? extends TestResult> getFailedTests() {
        return emptyList();
    }

    /**
     * Gets the "children" of this test result that passed
     *
     * @return the children of this test result, if any, or an empty collection
     */
    @Override
    public Collection<? extends TestResult> getPassedTests() {
        return listOnlyContainingThisObject;
    }

    /**
     * Gets the "children" of this test result that were skipped
     *
     * @return the children of this test result, if any, or an empty list
     */
    @Override
    public Collection<? extends TestResult> getSkippedTests() {
        return emptyList();
    }

    /**
     * Let's pretend that our trivial test result always passes.
     *
     * @return always true
     */
    @Override
    public boolean isPassed() {
        return true;
    }

    /**
     * Tests whether the test was skipped or not.
     *
     * @return true if the test was not executed, false otherwise.
     */
    public boolean isSkipped() {
        return false;
    }

    /**
     * Returns true iff this test failed.
     */
    public boolean isFailed() {
        return false;
    }

    /**
     * Time took to run this test. In seconds.
     */
    @Override
    public float getDuration() {
        return duration;
    }

    /**
     * Gets the name of this object.
     */
    @Override
    public String getName() {
        return "Simple Case Result";
    }

    /**
     * Gets the total number of passed tests.
     */
    @Override
    public int getPassCount() {
        return 1;
    }

    /**
     * Gets the total number of failed tests.
     */
    @Override
    public int getFailCount() {
        return 0;
    }

    /**
     * Gets the total number of skipped tests.
     */
    @Override
    public int getSkipCount() {
        return 0;
    }

    /**
     * Gets the human readable title of this result object.
     */
    @Override
    public String getTitle() {
        return "Simple Case Result";  //
    }

    public String getDisplayName() {
        return "Simple Case Result";
    }

    @Override
    public AbstractBuild<?, ?> getOwner() {
        if (parentAction == null) {
            LOGGER.warning("in Trivial Test Result, parentAction is null, but getOwner() called");
            return null;
        }
        return parentAction.owner;
    }

    @Override
    public List<TestAction> getTestActions() {
        return SimpleCaseResult.EMPTY_ACTION_LIST;
    }
    /**
     * An empty list of actions, useful for tests
     */
    public static final List<TestAction> EMPTY_ACTION_LIST = Collections.unmodifiableList(new ArrayList<TestAction>());
}