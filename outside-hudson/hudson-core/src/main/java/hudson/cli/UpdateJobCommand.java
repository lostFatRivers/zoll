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
import hudson.Extension;
import hudson.XmlFile;
import static hudson.cli.CreateJobCommand.isGoodName;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.util.IOUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.eclipse.hudson.security.team.Team;
import org.eclipse.hudson.security.team.TeamManager;
import org.eclipse.hudson.security.team.TeamManager.TeamNotFoundException;
import org.kohsuke.args4j.Argument;

/**
 * Updates or creates a job by reading stdin as a configuration XML file.
 *
 * @author Henrik Lynggaard Hansen
 */
@Extension
public class UpdateJobCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Updates and potentionally creates a job by reading stdin as a configuration XML file.";
    }
    // ?? name needs to be unqualified for create or qualified for update
    @Argument(metaVar = "NAME", usage = "Name of the job to update. Provide team qualified name if Team Management is enabled. Ex: team1.job1.", required = true)
    public String name;
    @Argument(metaVar = "CREATE", usage = "Create the job if needed, true|false", index = 1, required = true)
    public Boolean create;
    @Argument(metaVar = "TEAM", usage = "Team to create the job in (optional)", index = 2, required = false)
    public String team;

    protected int run() throws Exception {
        Team targetTeam = validateTeam(team, create, stderr);
        Hudson h = Hudson.getInstance();
        TeamManager teamManager = h.getTeamManager();
        
        if (team != null && targetTeam == null) {
            return -1;
        }

        String qualifiedJobName = !create ? name :
                (targetTeam == null
                    ? getNewJobName(name)
                    : teamManager.getRawTeamQualifiedJobName(targetTeam, name));
        TopLevelItem item = h.getItem(qualifiedJobName);

        if (item == null && !create) {
            stderr.println("Job '" + qualifiedJobName + "' does not exist and create is set to false");
            return -1;
        } else if (item != null && create) {
            stderr.println("Job '" + qualifiedJobName + "' already exists and create is set to true");
            return -1;
        }

        if (item == null) {
            name = name.trim();
            if (!isGoodName(name, stderr)) {
                return -1;
            }
            h.checkPermission(Item.CREATE);
            h.createProjectFromXML(name, team, stdin);
        } else {
            XmlFile oldConfigXml = null;
            Object oldItem = null;
            try {
                item.checkPermission(Job.CONFIGURE);
                File rootDirOfJob = teamManager.getRootFolderForJob(item.getName());
                // if the new config.xml is bad, need to restore the previous one
                oldConfigXml = Items.getConfigFile(rootDirOfJob);
                oldItem = oldConfigXml.read();
                // place it as config.xml
                File configXml = oldConfigXml.getFile();
                IOUtils.copy(stdin, configXml);

                h.reloadProjectFromDisk(configXml.getParentFile());
            } catch (IOException e) {
                if (oldConfigXml != null && oldItem != null) {
                    oldConfigXml.write(oldItem);
                }
                throw e;
            }
        }
        return 0;
    }
    
    /**
     * If team management enabled, return qualified job name;
     * otherwise, just the name.
     * @param name job name specified
     * @return job name that will be created
     */
    public static String getNewJobName(String name) {
        TeamManager teamManager = Hudson.getInstance().getTeamManager();
        if (teamManager.isTeamManagementEnabled()) {
            return teamManager.getRawTeamQualifiedJobName(name);
        }
        return name;
    }
    
    /**
     * Validate team exists and user can access it.
     * 
     * @param team team name
     * @param create true if create new job
     * @param stderr
     * @return 
     */
    public static Team validateTeam(String team, boolean create, PrintStream stderr) {
        Hudson h = Hudson.getInstance();
        TeamManager teamManager = h.getTeamManager();
        Team targetTeam = null;
        if (team != null) {
            if (!create) {
                stderr.println("team may only be used for create - for update use fully qualified name");
            } else if (!teamManager.isTeamManagementEnabled()) {
                stderr.println("team may not be specified unless team management is enabled");
            } else {
                try {
                    // check team exists first for better error message
                    targetTeam = teamManager.findTeam(team);
                    if (!teamManager.isCurrentUserHasAccessToTeam(team)) {
                        stderr.println("Current user does not have access to team "+team);
                        targetTeam = null;
                    }
                } catch (TeamNotFoundException e) {
                    stderr.println("Team "+team+" does not exist");
                }
            }
        }
        return targetTeam;
    }
}
