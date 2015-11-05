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
 *    Kohsuke Kawaguchi, Daniel Dyer, Red Hat, Inc., Tom Huybrechts, Yahoo!, Inc.
 *
 *
 *******************************************************************************/ 

package hudson.tasks.junit;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.util.HeapSpaceStringConverter;
import hudson.util.XStream2;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Action} that displays the JUnit test result.
 *
 * <p> The actual test reports are isolated by {@link WeakReference} so that it
 * doesn't eat up too much memory.
 *
 * @author Kohsuke Kawaguchi
 */
public class TestResultAction extends AbstractTestResultAction<TestResultAction> implements StaplerProxy {

    private static final Logger LOGGER = Logger.getLogger(TestResultAction.class.getName());
    private static final XStream XSTREAM = new XStream2();
    private transient WeakReference<TestResult> result;
    // Hudson < 1.25 didn't set these fields, so use Integer
    // so that we can distinguish between 0 tests vs not-computed-yet.
    private int failCount;
    private int skipCount;
    private Integer totalCount;
    private List<Data> testData = new ArrayList<Data>();

    public TestResultAction(AbstractBuild owner, TestResult result, BuildListener listener) {
        super(owner);
        setResult(result, listener);
    }

    /**
     * Overwrites the {@link TestResult} by a new data set.
     */
    public synchronized void setResult(TestResult result, BuildListener listener) {
        result.freeze(this);

        totalCount = result.getTotalCount();
        failCount = result.getFailCount();
        skipCount = result.getSkipCount();

        // persist the data
        try {
            getDataFile().write(result);
        } catch (IOException e) {
            e.printStackTrace(listener.fatalError("Failed to save the JUnit test result"));
        }

        this.result = new WeakReference<TestResult>(result);
    }

    private XmlFile getDataFile() {
        return new XmlFile(XSTREAM, new File(owner.getRootDir(), "junitResult.xml"));
    }

    public synchronized TestResult getResult() {
        TestResult r;
        if (result == null) {
            r = load();
            result = new WeakReference<TestResult>(r);
        } else {
            r = result.get();
        }

        if (r == null) {
            r = load();
            result = new WeakReference<TestResult>(r);
        }
        if (totalCount == null) {
            totalCount = r.getTotalCount();
            failCount = r.getFailCount();
            skipCount = r.getSkipCount();
        }
        return r;
    }

    @Override
    public synchronized int getFailCount() {
        if (totalCount == null) {
            getResult();    // this will compute the result
        }
        return failCount;
    }

    @Override
    public synchronized int getSkipCount() {
        if (totalCount == null) {
            getResult();    // this will compute the result
        }
        return skipCount;
    }

    @Override
    public synchronized int getTotalCount() {
        if (totalCount == null) {
            getResult();    // this will compute the result
        }
        return totalCount;
    }

    @Override
    public List<CaseResult> getFailedTests() {
        return getResult().getFailedTests();
    }

    /**
     * Loads a {@link TestResult} from disk.
     */
    private TestResult load() {
        TestResult r;
        try {
            r = (TestResult) getDataFile().read();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load " + getDataFile(), e);
            r = new TestResult();   // return a dummy
        }
        r.freeze(this);
        return r;
    }

    @Override
    public Object getTarget() {
        return getResult();
    }

    public List<TestAction> getActions(TestObject object) {
        List<TestAction> result = new ArrayList<TestAction>();
        // Added check for null testData to avoid NPE from issue 4257.
        if (testData != null) {
            for (Data data : testData) {
                result.addAll(data.getTestAction(object));
            }
        }
        return Collections.unmodifiableList(result);

    }

    public void setData(List<Data> testData) {
        this.testData = testData;
    }

    /**
     * Resolves {@link TestAction}s for the given {@link TestObject}.
     *
     * <p> This object itself is persisted as a part of {@link AbstractBuild},
     * so it needs to be XStream-serializable.
     *
     * @see TestDataPublisher
     */
    public static abstract class Data {

        /**
         * Returns all TestActions for the testObject.
         *
         * @return Can be empty but never null. The caller must assume that the
         * returned list is read-only.
         */
        public abstract List<? extends TestAction> getTestAction(hudson.tasks.junit.TestObject testObject);
    }

    public Object readResolve() {
        super.readResolve(); // let it do the post-deserialization work
        if (testData == null) {
            testData = new ArrayList<Data>();
        }

        return this;
    }

    static {
        XSTREAM.alias("result", TestResult.class);
        XSTREAM.alias("suite", SuiteResult.class);
        XSTREAM.alias("case", CaseResult.class);
        XSTREAM.registerConverter(new HeapSpaceStringConverter(), 100);
    }
}
