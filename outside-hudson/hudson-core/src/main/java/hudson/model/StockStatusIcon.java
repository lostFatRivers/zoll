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

package hudson.model;

import hudson.Functions;
import org.jvnet.localizer.LocaleProvider;
import org.jvnet.localizer.Localizable;

/**
 * {@link StatusIcon} for stock icon in Hudson.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.390.
 */
public final class StockStatusIcon extends AbstractStatusIcon {

    private final Localizable description;
    private final String image;

    /**
     * @param image Short file name like "folder.png" that points to a stock
     * icon in Hudson.
     * @param description Used as {@link #getDescription()}.
     */
    public StockStatusIcon(String image, Localizable description) {
        this.image = image;
        this.description = description;
    }

    public String getImageOf(String size) {
        return Functions.getRequestRootPath(null) + Hudson.RESOURCE_PATH + "/images/" + size + '/' + image;
    }

    public String getDescription() {
        return description.toString(LocaleProvider.getLocale());
    }
}
