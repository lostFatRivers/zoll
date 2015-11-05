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
 *    Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.api.matrix;

import hudson.matrix.AxisList;
import hudson.model.Result;
import java.io.IOException;
import org.eclipse.hudson.api.model.IBaseBuildableProject;

/**
 * Matrix Project Interface.
 *
 * @author Anton Kozak
 */
public interface IMatrixProject extends IBaseBuildableProject {

    /**
     * Returns {@link hudson.matrix.AxisList} of configured axes.
     *
     * @return {@link hudson.matrix.AxisList} of configured axes.
     */
    public AxisList getAxes();

    /**
     * Reconfigures axes.
     *
     * @param axes new {@link AxisList}.
     * @throws java.io.IOException exception.
     */
    public void setAxes(AxisList axes) throws IOException;

    /**
     * Whether Hudson should run {@link hudson.matrix.MatrixRun}s are run
     * sequentially.
     *
     * @return If true, {@link hudson.matrix.MatrixRun}s are run sequentially,
     * instead of running in parallel.
     */
    boolean isRunSequentially();

    /**
     * Sets the mode of the running.
     *
     * @param runSequentially If true, {@link hudson.matrix.MatrixRun}s are run
     * sequentially, instead of running in parallel.
     * @throws IOException exception.
     */
    void setRunSequentially(boolean runSequentially) throws IOException;

    /**
     * Sets the combination filter.
     *
     * @param combinationFilter the combinationFilter to set
     * @throws java.io.IOException exception.
     */
    void setCombinationFilter(String combinationFilter) throws IOException;

    /**
     * Obtains the combination filter, used to trim down the size of the matrix.
     * <p/>
     * <
     * p/> By default, a {@link hudson.matrix.MatrixConfiguration} is created
     * for every possible combination of axes exhaustively. But by specifying a
     * Dynamic Language Script expression as a combination filter, one can trim
     * down the # of combinations built.
     * <p/>
     * <
     * p/> Namely, this expression is evaluated for each axis value combination,
     * and only when it evaluates to true, a corresponding
     * {@link hudson.matrix.MatrixConfiguration} will be created and built.
     *
     * @return can be null.
     * @since 1.279
     */
    String getCombinationFilter();

    /**
     * Returns touchstone combination filter.
     *
     * @return touchstone combination filter.
     */
    String getTouchStoneCombinationFilter();

    /**
     * Sets touchstone combination filter.
     *
     * @param touchStoneCombinationFilter touchstone combination filter.
     */
    void setTouchStoneCombinationFilter(String touchStoneCombinationFilter);

    /**
     * Returns touchstone combination result condition.
     *
     * @return touchstone combination result condition.
     */
    Result getTouchStoneResultCondition();

    /**
     * Sets touchstone combination result condition.
     *
     * @param touchStoneResultCondition touchstone combination result condition.
     */
    void setTouchStoneResultCondition(Result touchStoneResultCondition);

    /**
     * Returns custom workspace.
     *
     * @return custom workspace.
     */
    String getCustomWorkspace();

    /**
     * Sets User-specified workspace directory, or null if it's up to Hudson.
     * <p/>
     * <
     * p/> Normally a matrix project uses the workspace location assigned by its
     * parent container, but sometimes people have builds that have hard-coded
     * paths.
     * <p/>
     * <
     * p/> This is not {@link java.io.File} because it may have to hold a path
     * representation on another OS.
     * <p/>
     * <
     * p/> If this path is relative, it's resolved against
     * {@link hudson.model.Node#getRootPath()} on the node where this workspace
     * is prepared.
     *
     * @param customWorkspace custom workspace.
     * @throws java.io.IOException exception.
     */
    void setCustomWorkspace(String customWorkspace) throws IOException;
}