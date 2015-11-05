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

package hudson.cli.handlers;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TopLevelItem;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * Refers to {@link TopLevelItem} by its name. Registered at META-INF/services.
 *
 * @author Kohsuke Kawaguchi
 */
public class TopLevelItemOptionHandler extends RequiresAuthenticationOptionHandler<TopLevelItem> {

    public TopLevelItemOptionHandler(CmdLineParser parser, OptionDef option, Setter<TopLevelItem> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        Hudson h = Hudson.getInstance();
        String src = params.getParameter(0);

        if (isAuthenticated()) {
            TopLevelItem s = h.getItem(src);
            if (s == null) {
                throw new CmdLineException(owner, "No such job '" + src + "' perhaps you meant " + AbstractProject.findNearest(src) + "?");
            }
            setter.addValue(s);
        }
        return 1;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "JOB";
    }
}
