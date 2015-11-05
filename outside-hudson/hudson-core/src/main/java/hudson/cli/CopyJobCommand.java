/*******************************************************************************
 *
 * Copyright (c) 2004-2010, Oracle Corporation.
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

package hudson.cli;

import hudson.model.Hudson;
import hudson.model.TopLevelItem;
import hudson.Extension;
import static hudson.cli.CreateJobCommand.isGoodName;
import static hudson.cli.UpdateJobCommand.getNewJobName;
import static hudson.cli.UpdateJobCommand.validateTeam;
import hudson.model.Item;
import org.eclipse.hudson.security.team.Team;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Copies a job from CLI.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class CopyJobCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Copies a job";
    }
    @Argument(metaVar = "SRC", usage = "Name of the job to copy. Provide team qualified name if Team Management is enabled. Ex: team1.job1.", required = true)
    public TopLevelItem src;
    @Argument(metaVar = "DST", usage = "Name of the new job to be created. The job name should not be team qualified. Ex: job1 ", index = 1, required = true)
    public String dst;
    @Argument(metaVar = "TEAM", usage = "Team to create the job in.", index = 2, required = false)
    public String team;
    @Option(name = "-fs", aliases = {"--force-save"}, usage = "Force saving the destination job in order to enable build functionality.")
    public boolean forceSave;

    protected int run() throws Exception {
        Hudson h = Hudson.getInstance();
        h.checkPermission(Item.CREATE);
        Team targetTeam = validateTeam(team, true, stderr);

        if (team != null && targetTeam == null) {
            return -1;
        }

        dst = dst.trim();
        if (!isGoodName(dst, stderr)) {
            return -1;
        }
            
        String qualifiedJobName = targetTeam == null
                ? getNewJobName(dst)
                : h.getTeamManager().getRawTeamQualifiedJobName(targetTeam, dst);
        if (h.getItem(qualifiedJobName) != null) {
                stderr.println("Job '" + qualifiedJobName + "' already exists");
            return -1;
        }

        TopLevelItem newJob = h.copy(src, dst, team);
        // Ensure this is a real job
        newJob = h.getItem(newJob.getName());
        if (forceSave && null != newJob) {
            newJob.save();
        }
        return 0;
    }
}
