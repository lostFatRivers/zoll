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

package org.eclipse.hudson.model.project.property;

import hudson.matrix.AxisList;
import org.eclipse.hudson.api.model.ICascadingJob;

/**
 * Represents {@link hudson.matrix.AxisList} property.
 * <p/>
 * Date: 10/5/11
 *
 * @author Anton Kozak
 */
public class AxisListProjectProperty extends BaseProjectProperty<AxisList> {

    public AxisListProjectProperty(ICascadingJob job) {
        super(job);
    }

    @Override
    public AxisList getDefaultValue() {
        return new AxisList();
    }
}