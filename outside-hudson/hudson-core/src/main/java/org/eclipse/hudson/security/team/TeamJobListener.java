/*
 * Copyright (c) 2013 Oracle Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Winston Prakash
 */
package org.eclipse.hudson.security.team;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import java.io.IOException;
import java.util.List;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.team.TeamManager.TeamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple helper to listen to job creation, deletion and renaming and
 * accordingly add, delete or rename it in the current users team.
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
@Extension
public class TeamJobListener extends ItemListener {

    private transient Logger logger = LoggerFactory.getLogger(TeamJobListener.class);

    @Override
    public void onCreated(Item item) {
        if (item instanceof Job<?, ?>) {
            if (!HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getTeamManager().isTeamManagementEnabled()) {
                addToPublicTeam(item.getName());
            } else if (getTeamManager().findJobOwnerTeam(item.getName()) == null) {
                // Job going to other than default user team must already be added, else...
                addToCurrentUserTeam(item.getName());
            }
        }
    }

    @Override
    public void onRenamed(Item item, String oldJobName, String newJobName) {
        if (item instanceof Job<?, ?>) {
            removeJob(oldJobName);
            if (!HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getTeamManager().isTeamManagementEnabled()) {
                addToPublicTeam(newJobName);
            } else if (getTeamManager().findJobOwnerTeam(newJobName) == null) {
                // Job going to other than default user team must already be added, else...
                addToCurrentUserTeam(newJobName);
            }
        }
    }

    @Override
    public void onDeleted(Item item) {
        if (item instanceof Job<?, ?>) {
            removeJob(item.getName());
        }
    }
    
    private void logFailedToAdd(String jobName, String teamName, Exception ex) {
        logger.error("Failed to add job "+jobName+" to "+teamName+" team", ex);
    }
    
    private void addToCurrentUserTeam(String jobName) {
        try {
            //getTeamManager().addJobToCurrentUserTeam(jobName);
            List<Team> userTeams = getTeamManager().getCurrentUserTeamsWithPermission(Item.CREATE); 
            if (!userTeams.isEmpty()){
                getTeamManager().addJob(userTeams.get(0), jobName);
            }else{
                // As a last resort add as a public scoped job
                addToPublicTeam(jobName);
            }
        } catch (IOException ex) {
            logFailedToAdd(jobName, "current user", ex);
        } catch (TeamNotFoundException ex) {
            logFailedToAdd(jobName, "current user", ex);
        }
    }
    
    private void addToPublicTeam(String jobName) {
        Team publicTeam = getTeamManager().getPublicTeam();
        try {
            publicTeam.addJob(new TeamJob(jobName));
        } catch (IOException ex) {
            logFailedToAdd(jobName, "public", ex);
        }
    }
    
    private void removeJob(String jobName) {
        Team team = getTeamManager().findJobOwnerTeam(jobName);
        if (team != null) {
            try {
                team.removeJob(jobName);
            } catch (IOException ex) {
                logger.error("Failed to remove job "+jobName+" in team"+team.getName(), ex);
            }
        }
    }

    private TeamManager getTeamManager() {
        return HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getTeamManager();
    }
}
