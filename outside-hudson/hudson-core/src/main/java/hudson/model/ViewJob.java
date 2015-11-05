/*******************************************************************************
 *
 * Copyright (c) 2004-2013 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi, Roy Varghese
 *
 *
 *******************************************************************************/ 

package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.SortedMap;

import hudson.model.Descriptor.FormException;

/**
 * {@link Job} that monitors activities that happen outside Hudson, which
 * requires occasional batch reload activity to obtain the up-to-date
 * information.
 *
 * <p> This can be used as a base class to derive custom {@link Job} type.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ViewJob<JobT extends ViewJob<JobT, RunT>, 
        RunT extends Run<JobT, RunT> & BuildNavigable>
        extends Job<JobT, RunT> {

    /**
     * We occasionally update the list of {@link Run}s from a file system. The
     * next scheduled update time.
     */
    private transient long nextUpdate = 0;
    /**
     * All {@link Run}s. Copy-on-write semantics.
     */
    protected transient /*almost final*/ RunMap<JobT,RunT> runs ;
    private transient boolean notLoaded = true;
    /**
     * If the reloading of runs are in progress (in another thread, set to
     * true.)
     */
    private transient volatile boolean reloadingInProgress;
    /**
     * {@link ExternalJob}s that need to be reloaded.
     *
     * This is a set, so no {@link ExternalJob}s are scheduled twice, yet it's
     * order is predictable, avoiding starvation.
     */
    private static final LinkedHashSet<ViewJob> reloadQueue = new LinkedHashSet<ViewJob>();
    /*package*/ static final Thread reloadThread = new ReloadThread();

    static {
        reloadThread.start();
    }

    /**
     * @deprecated as of 1.390
     */
    protected ViewJob(Hudson parent, String name) {
        super(parent, name);
        initRuns();
    }

    protected ViewJob(ItemGroup parent, String name) {
        super(parent, name);
        initRuns();
    }

    public boolean isBuildable() {
        return false;
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad(parent, name);
        notLoaded = true;
    }
    
    private void initRuns() {
        if (runs == null) {
            runs = new RunMap(this);
        }
    }
    
    protected SortedMap<Integer, RunT> _getRuns() {
        if (notLoaded || runs == null) {
            // if none is loaded yet, do so immediately.
            synchronized (this) {
                initRuns();
                if (notLoaded) {
                    notLoaded = false;
                    _reload();
                }
            }
        }
        if (nextUpdate < System.currentTimeMillis()) {
            if (!reloadingInProgress) {
                // schedule a new reloading operation.
                // we don't want to block the current thread,
                // so reloading is done asynchronously.
                reloadingInProgress = true;
                synchronized (reloadQueue) {
                    reloadQueue.add(this);
                    reloadQueue.notify();
                }
            }
        }
        return runs;
    }

    @Override
    public BuildHistory<JobT, RunT> getBuildHistoryData() {
        return (BuildHistory<JobT,RunT>)_getRuns();
    }
    
    public void removeRun(RunT run) {
        // reload the info next time
        nextUpdate = 0;
    }

    private void _reload() {
        try {
            reload();
        } finally {
            reloadingInProgress = false;
            nextUpdate = reloadPeriodically ? System.currentTimeMillis() + 1000 * 60 : Long.MAX_VALUE;
        }
    }

    /**
     * Reloads the list of {@link Run}s. This operation can take a long time.
     *
     * <p> The loaded {@link Run}s should be set to {@link #runs}.
     */
    protected abstract void reload();

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req, rsp);
        // make sure to reload to reflect this config change.
        nextUpdate = 0;
    }

    /**
     * Thread that reloads the {@link Run}s.
     */
    private static final class ReloadThread extends Thread {

        private ReloadThread() {
            setName("ViewJob reload thread");
        }

        private ViewJob getNext() throws InterruptedException {
            synchronized (reloadQueue) {
                // reload operations might eat InterruptException,
                // so check the status every so often
                while (reloadQueue.isEmpty() && !terminating()) {
                    reloadQueue.wait(60 * 1000);
                }
                if (terminating()) {
                    throw new InterruptedException();   // terminate now
                }
                ViewJob job = reloadQueue.iterator().next();
                reloadQueue.remove(job);
                return job;
            }
        }

        private boolean terminating() {
            return Hudson.getInstance().isTerminating();
        }

        @Override
        public void run() {
            while (!terminating()) {
                try {
                    getNext()._reload();
                } catch (InterruptedException e) {
                    // treat this as a death signal
                    return;
                } catch (Throwable t) {
                    // otherwise ignore any error
                    t.printStackTrace();
                }
            }
        }
    }
    // private static final Logger logger = Logger.getLogger(ViewJob.class.getName());
    /**
     * In the very old version of Hudson, an external job submission was just
     * creating files on the file system, so we needed to periodically reload
     * the jobs from a file system to pick up new records.
     *
     * <p> We then switched to submission via HTTP, so this reloading is no
     * longer necessary, so only do this when explicitly requested.
     *
     */
    public static boolean reloadPeriodically = Boolean.getBoolean(ViewJob.class.getName() + ".reloadPeriodically");
}