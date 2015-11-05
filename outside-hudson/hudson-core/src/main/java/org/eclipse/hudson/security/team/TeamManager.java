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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.BulkChange;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.AllView;
import hudson.model.Computer;
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.model.listeners.SaveableListener;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Manager that manages the teams and their persistence
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public final class TeamManager implements Saveable, AccessControlled {

    public static final String TEAM_SEPARATOR = ".";
    private final List<String> sysAdmins = new CopyOnWriteArrayList();
    private final List<Team> teams = new CopyOnWriteArrayList<Team>();
    private transient final XStream xstream = new XStream2();
    private final transient Logger logger = LoggerFactory.getLogger(TeamManager.class);
    private final transient File hudsonHomeDir;
    private final transient File teamsFolder;
    private transient final String teamsConfigFileName = "teams.xml";
    private transient PublicTeam publicTeam;
    private transient final String TEAMS_FOLDER_NAME = "teams";

    public TeamManager(File homeDir) {
        hudsonHomeDir = homeDir;
        teamsFolder = new File(hudsonHomeDir, TEAMS_FOLDER_NAME);
        if (!teamsFolder.exists()) {
            teamsFolder.mkdirs();
        }
        initializeXstream();
        load();
        ensurePublicTeam();
        ensureCustomFolders();
    }

    @Override
    public ACL getACL() {
        AuthorizationStrategy authorizationStrategy = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getAuthorizationStrategy();
        if (authorizationStrategy instanceof TeamBasedAuthorizationStrategy) {
            TeamBasedAuthorizationStrategy teamBasedAuthorizationStrategy = (TeamBasedAuthorizationStrategy) authorizationStrategy;
            return teamBasedAuthorizationStrategy.getACL(this);
        }
        // Team will not be used if Team Based Authorization Strategy is not used
        return new ACL() {
            @Override
            public boolean hasPermission(Authentication a, Permission permission) {
                return false;
            }
        };
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return getACL().hasPermission(permission);
    }

    @Override
    public void checkPermission(Permission permission) throws AccessDeniedException {
        getACL().checkPermission(permission);
    }

    public boolean isTeamManagementEnabled() {
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        if (hudsonSecurityManager != null) {
            AuthorizationStrategy authorizationStrategy = hudsonSecurityManager.getAuthorizationStrategy();
            if (authorizationStrategy instanceof TeamBasedAuthorizationStrategy) {
                return true;
            }
        }
        return false;
    }

    public void addSysAdmin(String adminName) throws IOException {
        if (!sysAdmins.contains(adminName)) {
            sysAdmins.add(adminName);
            save();
        }
    }

    public void removeSysAdmin(String adminName) throws IOException {
        if (sysAdmins.contains(adminName)) {
            sysAdmins.remove(adminName);
            save();
        }
    }

    public List<String> getSysAdmins() {
        return sysAdmins;
    }

    boolean isSysAdmin(String userName) {
        if (getTeamAwareSecurityRealm() != null) {
            return getTeamAwareSecurityRealm().isCurrentUserSysAdmin();
        } else {
            for (String admin : sysAdmins) {
                if (userName.equalsIgnoreCase(admin)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isCurrentUserSysAdmin() {
        String user = getCurrentUser();
        logger.debug("Checking if principal {} is a System Admin", user);
        if (isSysAdmin(user)) {
            return true;
        } else {
            for (GrantedAuthority ga : getCurrentUserRoles()) {
                logger.debug("Checking if the principal's role {} is a System Admin Role", ga.toString());
                if (isSysAdmin(ga.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isCurrentUserTeamAdmin(String teamName) {
        if (isCurrentUserSysAdmin()) {
            return true;
        }
        String user = getCurrentUser();
        try {
            Team team = findTeam(teamName);
            if (team.isAdmin(user)) {
                return true;
            } else {
                // Check if any of the group the user is a memmber has given
                // Team Admin Role
                for (GrantedAuthority ga : getCurrentUserRoles()) {
                    logger.debug("Checking if the principal's role {} is a Team Admin Role", ga.toString());
                    if (team.isAdmin(ga.getAuthority())) {
                        return true;
                    }
                }
            }
        } catch (TeamNotFoundException ex) {
            logger.info(ex.getLocalizedMessage(), ex);
        }
        return false;
    }
    
    // Used in Jelly
    public boolean isCurrentUserAnyTeamAdmin(){
        for (Team team : teams) {
            if (isCurrentUserTeamAdmin(team.getName())){
                return true;
            }
        }
        return false;
    }

    //Used by TeamManager Jelly to display team details in master-details fashion
    public Map<String, Team> getTeams() {
        Map<String, Team> teamMap = new HashMap<String, Team>();
        for (Team team : teams) {
            teamMap.put(team.getName(), team);
        }
        return teamMap;
    }

    public List<String> getTeamNames() {
        List<String> teamList = new ArrayList<String>();
        for (Team team : teams) {
            teamList.add(team.getName());
        }
        return teamList;
    }

    public Team createTeam(String teamName) throws IOException, TeamAlreadyExistsException {
        return createTeam(teamName, teamName, null);
    }

    public Team createTeam(String teamName, String description, String customFolder) throws IOException, TeamAlreadyExistsException {
        try {
            Hudson.checkGoodTeamName(teamName);
            if (teamName.trim().length() > Hudson.TEAM_NAME_LIMIT) {
                throw new Failure("Team name cannot exceed " + Hudson.TEAM_NAME_LIMIT + " characters.");
            }
        } catch (Failure ex) {
            throw new IOException(ex.getMessage());
        }
        return internalCreateTeam(teamName, description, customFolder);
    }

    private Team internalCreateTeam(String teamName, String description, String customFolder) throws IOException, TeamAlreadyExistsException {
        for (Team team : teams) {
            if (teamName.equals(team.getName())) {
                throw new TeamAlreadyExistsException(teamName);
            }
        }

        Team newTeam = new Team(teamName, description, customFolder, this);
        addTeam(newTeam);
        return newTeam;
    }

    private void addTeam(Team team) throws IOException {
        teams.add(team);
        save();
    }

    public void deleteTeam(String teamName) throws TeamNotFoundException, IOException {
        deleteTeam(teamName, false);
    }

    public void deleteTeam(String teamName, boolean deleteJobs) throws TeamNotFoundException, IOException {
        Team team = findTeam(teamName);
        if (Team.PUBLIC_TEAM_NAME.equals(team.getName())) {
            throw new IOException("Cannot delete public team");
        }
        for (TeamJob job : team.getJobs()) {
            TopLevelItem item = Hudson.getInstance().getItem(job.getId());
            if (item != null && (item instanceof Job)) {
                if (deleteJobs) {
                    try {
                        item.delete();
                    } catch (InterruptedException e) {
                        throw new IOException("Delete team " + team.getName() + " was interrupted");
                    }
                } else {
                    // Make deleted team jobs public
                    moveJob((Job) item, team, publicTeam, null);
                }
            }
        }
        teams.remove(team);
        save();

        File teamFolder = team.getTeamFolder(teamsFolder);
        if (teamFolder.exists() && teamFolder.isDirectory()) {
            Util.deleteContentsRecursive(teamFolder);
            Util.deleteFile(teamFolder);
        }
    }

    public Team findTeam(String teamName) throws TeamNotFoundException {
        for (Team team : teams) {
            if (teamName.equals(team.getName())) {
                return team;
            }
        }
        throw new TeamNotFoundException(teamName);
    }

    public void removeTeam(String teamName) throws IOException, TeamNotFoundException {
        Team team = findTeam(teamName);
        teams.remove(team);
        save();
    }

    public HttpResponse doCreateTeam(@QueryParameter String teamName, @QueryParameter String description, @QueryParameter String customFolder) throws IOException {
        if (!isCurrentUserSysAdmin()) {
            return new TeamUtils.ErrorHttpResponse("No permission to create team.");
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((customFolder != null) && !"".equals(customFolder.trim())) {
            File folder = new File(customFolder.trim());
            if (!folder.exists() && !folder.mkdirs()) {
                return new TeamUtils.ErrorHttpResponse("Could not create custom team folder - " + customFolder);
            }
        }
        try {
            Hudson.checkGoodName(teamName);
            if (teamName.trim().length() > Hudson.TEAM_NAME_LIMIT) {
                throw new Failure("Team name cannot exceed " + Hudson.TEAM_NAME_LIMIT + " characters.");
            }
        } catch (Failure ex) {
            return new TeamUtils.ErrorHttpResponse(ex.getMessage());
        }
        try {
            internalCreateTeam(teamName, description, customFolder);
            return HttpResponses.ok();
        } catch (TeamAlreadyExistsException ex) {
            return new TeamUtils.ErrorHttpResponse(ex.getLocalizedMessage());
        }
    }

    public HttpResponse doDeleteTeam(@QueryParameter String teamName) throws IOException {
        if (!isCurrentUserSysAdmin()) {
            return new TeamUtils.ErrorHttpResponse("No permission to delete team.");
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        try {
            deleteTeam(teamName);
            return HttpResponses.ok();
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(ex.getLocalizedMessage());
        }
    }

    public HttpResponse doAddTeamMember(@QueryParameter String teamName,
            @QueryParameter String teamMemberSid,
            @QueryParameter boolean isTeamAdmin,
            @QueryParameter boolean canCreate,
            @QueryParameter boolean canDelete,
            @QueryParameter boolean canConfigure,
            @QueryParameter boolean canBuild,
            @QueryParameter boolean canCreateNode,
            @QueryParameter boolean canDeleteNode,
            @QueryParameter boolean canConfigureNode,
            @QueryParameter boolean canCreateView,
            @QueryParameter boolean canDeleteView,
            @QueryParameter boolean canConfigureView
    ) throws IOException {
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to add team member.");
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((teamMemberSid == null) || "".equals(teamMemberSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team member name required.");
        }
        Team team;
        try {
            team = findTeam(teamName);
            if (team.findMember(teamMemberSid) == null) {
                team.addMember(teamMemberSid, isTeamAdmin, canCreate, canDelete, canConfigure, canBuild,
                        canCreateNode, canDeleteNode, canConfigureNode, canCreateView, canDeleteView, canConfigureView);
                return FormValidation.respond(FormValidation.Kind.OK, TeamUtils.getIcon(teamMemberSid));
            } else {
                return new TeamUtils.ErrorHttpResponse(teamMemberSid + " is already a team member.");
            }
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }
    }

    public HttpResponse doUpdateTeamMember(@QueryParameter String teamName,
            @QueryParameter String teamMemberSid,
            @QueryParameter boolean isTeamAdmin,
            @QueryParameter boolean canCreate,
            @QueryParameter boolean canDelete,
            @QueryParameter boolean canConfigure,
            @QueryParameter boolean canBuild,
            @QueryParameter boolean canCreateNode,
            @QueryParameter boolean canDeleteNode,
            @QueryParameter boolean canConfigureNode,
            @QueryParameter boolean canCreateView,
            @QueryParameter boolean canDeleteView,
            @QueryParameter boolean canConfigureView) throws IOException {
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to add team member.");
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((teamMemberSid == null) || "".equals(teamMemberSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team member name required.");
        }
        Team team;
        try {
            team = findTeam(teamName);
            TeamMember currentMember = team.findMember(teamMemberSid);
            if (currentMember != null) {
                team.updateMember(teamMemberSid, isTeamAdmin, canCreate, canDelete, canConfigure, canBuild,
                        canCreateNode, canDeleteNode, canConfigureNode, canCreateView, canDeleteView, canConfigureView);
                return FormValidation.respond(FormValidation.Kind.OK, TeamUtils.getIcon(teamMemberSid));
            } else {
                return new TeamUtils.ErrorHttpResponse(teamMemberSid + " is not a team member.");
            }
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

    }

    public HttpResponse doRemoveTeamMember(@QueryParameter String teamName,
            @QueryParameter String teamMemberSid) throws IOException {
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to remove team member");
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((teamMemberSid == null) || "".equals(teamMemberSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team member name required.");
        }
        Team team;
        try {
            team = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }
        TeamMember currentMember = team.findMember(teamMemberSid);
        if (currentMember != null) {
            team.removeMember(teamMemberSid);
            return HttpResponses.ok();
        } else {
            return new TeamUtils.ErrorHttpResponse(teamMemberSid + " is not a team member.");
        }
    }

    public HttpResponse doMoveJob(@QueryParameter String jobName, @QueryParameter String teamName) throws IOException {
        
        Team jobTeam = findJobOwnerTeam(jobName);
        
        if (!isCurrentUserTeamAdmin(jobTeam.getName())) {
            return new TeamUtils.ErrorHttpResponse("No permission to move job from team - " + jobTeam.getName());
        }
        
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to move job to team - " + teamName);
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((jobName == null) || "".equals(jobName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Job name required.");
        }

        Team newTeam;
        try {
            newTeam = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        Team oldTeam = findJobOwnerTeam(jobName);
        if (oldTeam == null) {
            return new TeamUtils.ErrorHttpResponse(jobName + " does not belong to any team.");
        }

        if (oldTeam == newTeam) {
            return new TeamUtils.ErrorHttpResponse(jobName + " is already in team " + oldTeam.getName());
        }

        Item item = Hudson.getInstance().getItem(jobName);
        Job job;
        if (item instanceof Job<?, ?>) {
            job = (Job) item;
            if (job.isBuilding()) {
                return new TeamUtils.ErrorHttpResponse(job.getName() + " is building.");
            }
            try {
                String newJobName = moveJob(job, oldTeam, newTeam, null);
                return HttpResponses.plainText(newJobName); 
            } catch (IOException ex) {
                return new TeamUtils.ErrorHttpResponse("Faile to move the job " + jobName
                        + " to the team " + teamName + ". " + ex.getLocalizedMessage());
            }
        } else {
            return new TeamUtils.ErrorHttpResponse(jobName + " not a valid job Id.");
        }
    }

    public HttpResponse doMoveView(@QueryParameter String viewName, @QueryParameter String teamName) throws IOException {
        Team viewTeam = findViewOwnerTeam(viewName);
        
        if (!isCurrentUserTeamAdmin(viewTeam.getName())) {
            return new TeamUtils.ErrorHttpResponse("No permission to move view from team - " + viewTeam.getName());
        }
        
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to move view to team - " + teamName);
        }
        
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((viewName == null) || "".equals(viewName.trim())) {
            return new TeamUtils.ErrorHttpResponse("View name required.");
        }

        Team newTeam;
        try {
            newTeam = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        Team oldTeam = findViewOwnerTeam(viewName);
        if (oldTeam == null) {
            return new TeamUtils.ErrorHttpResponse(viewName + " does not belong to any team.");
        }

        if (oldTeam == newTeam) {
            return new TeamUtils.ErrorHttpResponse(viewName + " is already in team " + oldTeam.getName());
        }
        moveView(oldTeam, newTeam, viewName);
        return HttpResponses.ok();
    }
    
    public void moveView(Team oldTeam, Team newTeam, String viewName) throws IOException {
        if (!oldTeam.removeView(viewName)) {
            throw new IOException("View "+viewName+" is not a member of team "+oldTeam.getName());
        }
        newTeam.addView(new TeamView(viewName));
    }


    public HttpResponse doMoveNode(@QueryParameter String nodeName, @QueryParameter String teamName) throws IOException {
         Team nodeTeam = findNodeOwnerTeam(nodeName);
        
        if (!isCurrentUserTeamAdmin(nodeTeam.getName())) {
            return new TeamUtils.ErrorHttpResponse("No permission to move node from team - " + nodeTeam.getName());
        }
        
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to move node to team - " + teamName);
        }
        
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required.");
        }
        if ((nodeName == null) || "".equals(nodeName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Node name required.");
        }

        Team newTeam;
        try {
            newTeam = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        Team oldTeam = findNodeOwnerTeam(nodeName);
        if (oldTeam == null) {
            return new TeamUtils.ErrorHttpResponse(nodeName + " does not belong to any team.");
        }

        if (oldTeam == newTeam) {
            return new TeamUtils.ErrorHttpResponse(nodeName + " is already in team " + oldTeam.getName());
        }
        moveNode(oldTeam, newTeam, nodeName);
        return HttpResponses.ok();
    }
    
    public void moveNode(Team oldTeam, Team newTeam, String nodeName) throws IOException {
        if (!oldTeam.removeNode(nodeName)) {
            throw new IOException("Node "+nodeName+" is not a member of team "+oldTeam.getName());
        }
        newTeam.addNode(new TeamNode(nodeName));
    }

    public HttpResponse doSetJobVisibility(@QueryParameter String jobName, @QueryParameter String teamNames, @QueryParameter boolean canViewConfig) throws IOException {
        Team jobTeam = findJobOwnerTeam(jobName);
        
        if (!isCurrentUserTeamAdmin(jobTeam.getName())) {
            return new TeamUtils.ErrorHttpResponse("No permission to set job visibility. Job name - " + jobName + " Team Name - " + jobTeam.getName());
        }
        if ((jobName == null) || "".equals(jobName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Job id required.");
        }
        Team ownerTeam = findJobOwnerTeam(jobName);
        if (ownerTeam == null) {
            return new TeamUtils.ErrorHttpResponse(jobName + " does not belong to any team.");
        } else {
            TeamJob job = ownerTeam.findJob(jobName);
            job.removeAllVisibilities();
            for (String teamName : teamNames.split(":")) {
                job.addVisibility(teamName);
            }
            job.setAllowConfigView(canViewConfig);
            save();
        }
        return HttpResponses.ok();
    }

    public HttpResponse doSetViewVisibility(@QueryParameter String viewName, @QueryParameter String teamNames) throws IOException {
        Team viewTeam = findViewOwnerTeam(viewName);
        if (!isCurrentUserTeamAdmin(viewTeam.getName())) {
            return new TeamUtils.ErrorHttpResponse("No permission to set view visibility. View name - " + viewName + " Team Name - " + viewTeam.getName());
        }
        if ((viewName == null) || "".equals(viewName.trim())) {
            return new TeamUtils.ErrorHttpResponse("View name required.");
        }
        Team ownerTeam = findViewOwnerTeam(viewName);
        if (ownerTeam == null) {
            return new TeamUtils.ErrorHttpResponse(viewName + " does not belong to any team.");
        } else {
            TeamView view = ownerTeam.findView(viewName);
            view.removeAllVisibilities();
            for (String teamName : teamNames.split(":")) {
                view.addVisibility(teamName);
            }
            save();
        }
        return HttpResponses.ok();
    }
    
    public HttpResponse doSetPrimaryView(@QueryParameter String viewName, @QueryParameter String teamName) throws IOException, TeamNotFoundException {
        Team viewTeam = findViewOwnerTeam(viewName);
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to set primary view. View name - " + viewName + " Team Name - " + viewTeam.getName());
        }
        if ((viewName == null) || "".equals(viewName.trim())) {
            return new TeamUtils.ErrorHttpResponse("View name required.");
        }
        
        Team viewVisibleTeam = findTeam(teamName);
        
        viewVisibleTeam.setPrimaryView(viewName);
       
        return HttpResponses.ok();
    }
    
    public void addViewVisibility(TeamView view, String teamName) {
        view.addVisibility(teamName);
    }

    public HttpResponse doSetNodeVisibility(@QueryParameter String nodeName, @QueryParameter String teamNames) throws IOException {
        Team nodeTeam = findNodeOwnerTeam(nodeName);
        if (!isCurrentUserTeamAdmin(nodeTeam.getName())) {
            return new TeamUtils.ErrorHttpResponse("No permission to set node visibility. Node name - " + nodeName + " Team Name - " + nodeTeam.getName());
        }
        if ((nodeName == null) || "".equals(nodeName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Node name required.");
        }
        Team ownerTeam = findNodeOwnerTeam(nodeName);
        if (ownerTeam == null) {
            return new TeamUtils.ErrorHttpResponse(nodeName + " does not belong to any team.");
        } else {
            TeamNode node = ownerTeam.findNode(nodeName);
            node.removeAllVisibilities();
            for (String teamName : teamNames.split(":")) {
                node.addVisibility(teamName);
            }
            save();
        }
        return HttpResponses.ok();
    }
    
    public HttpResponse doSetNodeEnabled(@QueryParameter String nodeName, @QueryParameter String teamName, @QueryParameter boolean enabled) throws IOException, TeamNotFoundException {
        if (!isCurrentUserTeamAdmin(teamName)) {
            return new TeamUtils.ErrorHttpResponse("No permission to enable node. Node name - " + nodeName + " Team Name - " + teamName);
        }
        if ((nodeName == null) || "".equals(nodeName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Node name required.");
        }
        Team nodeVisibleTeam = findTeam(teamName);
        
        setNodeEnabled(nodeName, nodeVisibleTeam, enabled);
        return HttpResponses.ok();
    }
    
    public void setNodeEnabled(String nodeName, Team team, boolean enabled) throws IOException {
        if (!enabled) {
            team.removeFromEnabledVisibleNodes(nodeName);
        } else {
            team.addToEnabledVisibleNodes(nodeName);
        }
    }
    
    public void addNodeVisibility(TeamNode node, String teamName) {
        node.addVisibility(teamName);
    }

    public HttpResponse doCheckSid(@QueryParameter String sid) throws IOException {
        return FormValidation.respond(FormValidation.Kind.OK, TeamUtils.getIcon(sid));
    }

    /**
     * Copy jobs from old team to new team. Supplying the original name helps,
     * when job is created for one team and then moved to another team (Ex.
     * Create Job in a team). When the job is created in first team it may take
     * a unique name different from the supplied original name.
     *
     * @param job
     * @param oldTeam
     * @param newTeam
     * @param originalName - original name with which the moved job should be
     * created.
     * @throws IOException
     */
    private String moveJob(Job job, Team oldTeam, Team newTeam, String originalName) throws IOException {
        try {
            String oldJobName = job.getName();
            String unqualifiedJobName = originalName;
            if ((originalName == null) || "".equals(originalName.trim())) {
                unqualifiedJobName = getUnqualifiedJobName(oldTeam, job.getName());
            }
            unqualifiedJobName = unqualifiedJobName.trim();
            // Deal with public job name created with team disabled corner case.
            if (isPublicTeam(oldTeam)) {
                // Job name might be too long or contain team separator
                if (unqualifiedJobName.length() > Hudson.JOB_NAME_LIMIT_TEAM) {
                    unqualifiedJobName = unqualifiedJobName.substring(0, Hudson.JOB_NAME_LIMIT_TEAM);
                }
                unqualifiedJobName = unqualifiedJobName.replace(TEAM_SEPARATOR, "_");
            }
            String qualifiedNewJobName = getTeamQualifiedJobName(newTeam, unqualifiedJobName);

            // Add the new job, rename before removing the old job
            // ensures team manager will find correct locations.
            newTeam.addJob(new TeamJob(qualifiedNewJobName));
            job.renameTo(qualifiedNewJobName);
            oldTeam.removeJob(oldJobName);

            Hudson.getInstance().replaceItem(job.getName(), qualifiedNewJobName);
            return qualifiedNewJobName;
        } catch (Exception exc) {
            throw new IOException(exc);
        }
    }

    /**
     * Before a job is created in a team, it must be added to the team so the
     * correct location will be found.
     *
     * @param unqualifiedJobName job name with no team qualification
     * @param team team the job is to be created in
     * @return qualified job name to be used to create
     * @throws IOException
     */
    public String addJob(String unqualifiedJobName, Team team) throws IOException {
        String qualifiedNewJobName = getTeamQualifiedJobName(team, unqualifiedJobName);
        team.addJob(new TeamJob(qualifiedNewJobName));
        return qualifiedNewJobName;
    }

    /**
     * Strip team qualification from job name.
     *
     * @param team must not be null
     * @param jobName qualified job name
     * @return unqualified job name
     */
    public String getUnqualifiedJobName(Team team, String jobName) {
        if (!Team.PUBLIC_TEAM_NAME.equals(team.getName()) && jobName.startsWith(team.getName() + TEAM_SEPARATOR)) {
            return jobName.substring(team.getName().length() + 1);
        }
        return jobName;
    }

    /**
     * Get the name of the current user admin teams as JSON
     *
     * @return HttpResponse with JSON as content type
     */
    public HttpResponse doGetTeamsJson() {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest sr, StaplerResponse rsp, Object o) throws IOException, ServletException {
                writeJson(rsp, (List<String>) getCurrentUserAdminTeams());
            }
        };

    }

    /**
     * Get names of all teams in TeamManager as JSON
     *
     * @return HttpResponse with JSON as content type
     */
    public HttpResponse doGetAllTeamsJson() {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest sr, StaplerResponse rsp, Object o) throws IOException, ServletException {
                writeJson(rsp, getTeamNames());
            }
        };
    }
    
    /**
     * Get the name of the views visible to the team as JSON
     *
     * @param teamName
     * @return HttpResponse with JSON as content type
     */
    public HttpResponse doGetViewsJson(@QueryParameter final String teamName) {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest sr, StaplerResponse rsp, Object o) throws IOException, ServletException {
                try {
                    Team team = findTeam(teamName);
                    writeJson(rsp, (List<String>) team.getAllViewNames());
                } catch (TeamNotFoundException ex) {
                     
                }
            }
        };

    }

    private void writeJson(StaplerResponse rsp, List<String> data) throws IOException {
        rsp.setStatus(HttpServletResponse.SC_OK);
        rsp.setContentType("application/json");
        PrintWriter w = new PrintWriter(rsp.getWriter());
        w.println("{");
        for (int i = 0; i < data.size(); i++) {
            w.print("\"" + data.get(i) + "\":\"" + data.get(i) + "\"");
            if (i < data.size() - 1) {
                w.println(",");
            }
        }
        w.println("}");
        w.close();
    }

    /* For Unit Test */
    void addUser(String teamName, String userName) throws TeamNotFoundException, IOException {
        Team team = findTeam(teamName);
        team.addMember(userName, false, false, false, false, false, false, false, false, false, false, false);
        save();
    }

    /**
     * Return true if current user has access to team. Team management must be
     * enabled.
     *
     * @param teamName
     * @return
     */
    public boolean isCurrentUserHasAccessToTeam(String teamName) {
        if (isTeamManagementEnabled()) {
            try {
                if (isCurrentUserSysAdmin()) {
                    return true;
                }
                Team team = findTeam(teamName);
                if (getTeamAwareSecurityRealm() != null) {
                    if (team.equals(getTeamAwareSecurityRealm().GetCurrentUserTeam())) {
                        return true;
                    }
                }
                for (Team userTeam : findCurrentUserTeams()) {
                    if (userTeam == team) {
                        return true;
                    }
                }
            } catch (TeamNotFoundException ex) {
                // no access
            }
        }
        return false;
    }

    private String getCurrentUser() {
        Authentication authentication = HudsonSecurityManager.getAuthentication();
        return authentication.getName();
    }

    private Collection<? extends GrantedAuthority> getCurrentUserRoles() {
        Authentication authentication = HudsonSecurityManager.getAuthentication();
        return authentication.getAuthorities();
    }

    /**
     * Check if current user is not sys admin and is admin of exactly one team.
     *
     * @return
     */
    public boolean isCurrentUserAdminInSingleTeam() {
        return getCurrentUserAdminTeams().size() == 1;
    }

    /**
     * Check if current user is admin in more than one team
     */
    public boolean isCurrentUserAdminInMultipleTeams() {
        return getCurrentUserAdminTeams().size() > 1;
    }

    /**
     * Get the team in the case where current user is admin of only one team.
     */
    public String getCurrentUserAdminTeam() {
        List<String> teams = (List<String>) getCurrentUserAdminTeams();
        if (teams.size() == 1) {
            return teams.get(0);
        }
        throw new IllegalStateException("Current user is admin of " + teams.size() + " teams");
    }

    /**
     * Get all the teams current user is admin of. Sys admin is considered to be
     * admin of all teams.
     */
    public Collection<String> getCurrentUserAdminTeams() {
        List<String> list = new ArrayList<String>();
        for (Team team : teams) {
            if (isCurrentUserTeamAdmin(team.getName())){
                list.add(team.getName());
            }
        }
        Collections.sort(list);
        return list;
    }

    //Used by teamManager.jelly. 
    public Collection<String> getCurrentUserAdminJobs() {
        Hudson hudson = Hudson.getInstance();
        List<String> jobNames = new ArrayList<String>();
        for (Team team : teams) {
             boolean teamAdmin = isCurrentUserTeamAdmin(team.getName());
            if (teamAdmin) {
                for (TeamJob teamJob : team.getJobs()) {
                    TopLevelItem item = hudson.getItem(teamJob.getId());
                    if (item != null && (item instanceof Job)) {
                        jobNames.add(item.getName());
                    }
                }
            }
        }
        Collections.sort(jobNames);
        return jobNames;
    }

    //Used by teamManager.jelly. 
    public Collection<String> getCurrentUserAdminViews() throws IOException {
        List<String> viewNames = new ArrayList<String>();
        for (Team team : teams) {
            boolean teamAdmin = isCurrentUserTeamAdmin(team.getName());
            if (teamAdmin) {
                for (TeamView teamView : team.getViews()) {
                    viewNames.add(teamView.getId());
                }
            }
        }
        Collections.sort(viewNames);
        return viewNames;
    }

    //Used by teamManager.jelly. 
    public Collection<String> getCurrentUserAdminNodes() throws IOException {
        List<String> nodeNames = new ArrayList<String>();
        for (Team team : teams) {
            boolean teamAdmin = isCurrentUserTeamAdmin(team.getName());
            if (teamAdmin) {
                for (TeamNode teamNode : team.getNodes()) {
                    nodeNames.add(teamNode.getId());
                }
            }
        }
        Collections.sort(nodeNames);
        return nodeNames;
    }

    /**
     * Check if current user is in more than one team.
     */
    public boolean isCurrentUserInMultipleTeams() {
        return getCurrentUserTeams().size() > 1;
    }

    /**
     * Get all the teams current user is a member of. Sys admin is considered to
     * be a member of all teams.
     */
    public Collection<String> getCurrentUserTeams() {
        List<String> list = new ArrayList<String>();
        boolean admin = isCurrentUserSysAdmin();
        String user = getCurrentUser();
        for (Team team : teams) {
            if (admin || team.isMember(user)) {
                list.add(team.getName());
            } else {
                // Check if any of the group the user is a memmber, is also a member of the team
                for (GrantedAuthority ga : getCurrentUserRoles()) {
                    logger.debug("Checking if the principal's role {} is a Team Admin Role", ga.toString());
                    if (team.isMember(ga.getAuthority())) {
                        list.add(team.getName());
                    }
                }
            }
        }
        return list;
    }

    // Used in hudson.model.view.newJob.jelly
    public Collection<String> getCurrentUserTeamsWithCreatePermission() {
        List<Team> teamsWithPermission;
        if (isCurrentUserSysAdmin()) {
            teamsWithPermission = teams;
        } else {
            teamsWithPermission = getCurrentUserTeamsWithPermission(Item.CREATE);
        }
        List<String> teamNames = new ArrayList<String>();
        for (Team team : teamsWithPermission) {
            teamNames.add(team.getName());
        }
        return teamNames;
    }

    public List<Team> findUserTeams(String userName) {
        List<Team> userTeams = new ArrayList<Team>();

        //Check if we have to use TeamAwareSecurityRealm
        if (getTeamAwareSecurityRealm() != null) {
            userTeams.add(getTeamAwareSecurityRealm().GetCurrentUserTeam());
            return userTeams;
        }
        for (Team team : teams) {
            if (team.isMember(userName)) {
                userTeams.add(team);
            }
        }
        return userTeams;
    }

    public Team findUserTeamForItem(String userName) {
        List<Team> userTeams = findUserTeams(userName);
        if (userTeams.isEmpty()) {
            return publicTeam;
        }
        return userTeams.get(0);
    }

    public Team findUserTeamForJob(String userName) {
        return findUserTeamForItem(userName);
    }

    public Team findUserTeamForView(String userName) {
        return findUserTeamForItem(userName);
    }

    public Team findUserTeamForNode(String userName) {
        return findUserTeamForItem(userName);
    }

    private TeamAwareSecurityRealm getTeamAwareSecurityRealm() {
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        if (hudsonSecurityManager != null) {
            SecurityRealm securityRealm = hudsonSecurityManager.getSecurityRealm();
            if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
                return (TeamAwareSecurityRealm) securityRealm;
            }
        }
        return null;
    }

    // this could be private
    public List<Team> findCurrentUserTeams() {

        //Check if we have to use TeamAwareSecurityRealm
        if (getTeamAwareSecurityRealm() != null) {
            return Arrays.asList(getTeamAwareSecurityRealm().GetCurrentUserTeam());
        }

        Authentication authentication = HudsonSecurityManager.getAuthentication();
        List<Team> userTeams = findUserTeams(authentication.getName());
        Collection<? extends GrantedAuthority> gas = authentication.getAuthorities();
        if (gas == null) {
            throw new IllegalStateException("authentication.getAuthorities() returned null array");
        }
        for (GrantedAuthority ga : gas) {
            String grantedAuthority = ga.getAuthority();
            userTeams.addAll(findUserTeams(grantedAuthority));
        }
        return userTeams;
    }

    public Team findJobOwnerTeam(String jobName) {
        for (Team team : teams) {
            if (team.isJobOwner(jobName)) {
                return team;
            }
        }
        return null;
    }

    public TeamJob findJob(String jobName) {
        Team jobTeam = findJobOwnerTeam(jobName);
        if (jobTeam != null) {
            return jobTeam.findJob(jobName);
        }
        return null;
    }

    public void addJob(Team team, String jobName) throws IOException, TeamNotFoundException {
        if (team != null) {
            team.addJob(new TeamJob(jobName));
        } else {
            findTeam(Team.PUBLIC_TEAM_NAME).addJob(new TeamJob(jobName));
        }
        save();
    }

    public void renameJob(Team team, String oldJobName, String newJobName) throws IOException {
        if (team != null) {
            team.renameJob(oldJobName, newJobName);
            save();
        }
    }

    public void removeJob(Team team, String jobName) throws IOException {
        if (team != null) {
            team.removeJob(jobName);
            save();
        }
    }

    public void removeJob(String jobName) throws IOException {
        removeJob(findJobOwnerTeam(jobName), jobName);
    }

    public void addJobToUserTeam(String userName, String jobName) throws IOException, TeamNotFoundException {
        // Fix bug in hudson.model.listeners.ItemListenerTest - no team found for user
        addJob(findUserTeamForJob(userName), jobName);
    }

    public void addJobToCurrentUserTeam(String jobName) throws IOException, TeamNotFoundException {
        addJobToUserTeam(getCurrentUser(), jobName);
    }

    void removeJobFromUserTeam(String userName, String jobName) throws IOException {
        // Used only in tests
        removeJob(findUserTeams(userName).get(0), jobName);
    }

    void renameJobInUserTeam(String userName, String oldJobName, String newJobName) throws IOException {
        // Used only in tests
        renameJob(findUserTeams(userName).get(0), oldJobName, newJobName);
    }
    
    public Team findNodeOwnerTeam(String nodeName) {
        for (Team team : teams) {
            if (team.isNodeOwner(nodeName)) {
                return team;
            }
        }
        return null;
    }

    public void addNode(Team team, String nodeName) throws IOException, TeamNotFoundException {
        if (team != null) {
            team.addNode(new TeamNode(nodeName));
        } else {
            findTeam(Team.PUBLIC_TEAM_NAME).addNode(new TeamNode(nodeName));
        }
        save();
    }

    public void renameNode(Team team, String oldNodeName, String newNodeName) throws IOException {
        if (team != null) {
            team.renameNode(oldNodeName, newNodeName);
            save();
        }
    }

    public void removeNode(Team team, String nodeName) throws IOException {
        if (team != null) {
            team.removeNode(nodeName);
            save();
        }
    }

    public TeamNode findNode(String nodeName) {
        Team nodeTeam = findNodeOwnerTeam(nodeName);
        if (nodeTeam != null) {
            return nodeTeam.findNode(nodeName);
        }
        return null;
    }

    public void removeNode(String nodeName) throws IOException {
        removeNode(findNodeOwnerTeam(nodeName), nodeName);
    }

    public void addNodeToUserTeam(String userName, String nodeName) throws IOException, TeamNotFoundException {
        // Fix bug in hudson.model.listeners.ItemListenerTest - no team found for user
        addNode(findUserTeamForNode(userName), nodeName);
    }

    public void addNodeToCurrentUserTeam(String nodeName) throws IOException, TeamNotFoundException {
        addNodeToUserTeam(getCurrentUser(), nodeName);
    }

    void removeNodeFromUserTeam(String userName, String nodeName) throws IOException {
        // Used only in tests
        removeNode(findUserTeams(userName).get(0), nodeName);
    }

    void renameNodeInUserTeam(String userName, String oldNodeName, String newNodeName) throws IOException {
        // Used only in tests
        renameNode(findUserTeams(userName).get(0), oldNodeName, newNodeName);
    }
    
    public Team findViewOwnerTeam(String viewName) {
        for (Team team : teams) {
            if (team.isViewOwner(viewName)) {
                return team;
            }
        }
        return null;
    }

    public TeamView findView(String viewName) {
        Team viewTeam = findViewOwnerTeam(viewName);
        if (viewTeam != null) {
            return viewTeam.findView(viewName);
        }
        return null;
    }

    public void addView(Team team, String viewName) throws IOException, TeamNotFoundException {
        if (team != null) {
            team.addView(new TeamView(viewName));
        } else {
            findTeam(Team.PUBLIC_TEAM_NAME).addView(new TeamView(viewName));
        }
        save();
    }

    public void renameView(Team team, String oldViewName, String newViewName) throws IOException {
        if (team != null) {
            team.renameView(oldViewName, newViewName);
            save();
        }
    }

    public void removeView(Team team, String viewName) throws IOException {
        if (team != null) {
            team.removeView(viewName);
            save();
        }
    }

    public void removeView(String viewName) throws IOException {
        removeView(findViewOwnerTeam(viewName), viewName);
    }

    public void addViewToUserTeam(String userName, String viewName) throws IOException, TeamNotFoundException {
        // Fix bug in hudson.model.listeners.ItemListenerTest - no team found for user
        addView(findUserTeamForView(userName), viewName);
    }

    public void addViewToCurrentUserTeam(String viewName) throws IOException, TeamNotFoundException {
        addViewToUserTeam(getCurrentUser(), viewName);
    }

    void removeViewFromUserTeam(String userName, String viewName) throws IOException {
        // Used only in tests
        removeView(findUserTeams(userName).get(0), viewName);
    }

    void renameViewInUserTeam(String userName, String oldViewName, String newViewName) throws IOException {
        // Used only in tests
        renameView(findUserTeams(userName).get(0), oldViewName, newViewName);
    }
    
    /**
     * Save the settings to the configuration file.
     * @throws java.io.IOException
     */
    @Override
    public synchronized void save() throws IOException {
        if (useBulkSaveFlag && BulkChange.contains(this)) {
            return;
        }
        getConfigFile().write(this);
        if (useBulkSaveFlag) {
            SaveableListener.fireOnChange(this, getConfigFile());
        }
    }

    /**
     * Get the current user team qualified Id for the job name If the user is
     * member of multiple teams use the first team
     *
     * @param jobName
     * @return String, Qualified Job ID
     */
    public String getTeamQualifiedJobName(String jobName) {
        List<Team> currentUserTeamsWithPermission = getCurrentUserTeamsWithPermission(Item.CREATE);
        if (!currentUserTeamsWithPermission.isEmpty()) {
            return getTeamQualifiedJobName(currentUserTeamsWithPermission.get(0), jobName);
        }
        return jobName;
    }

    /**
     * Called to check duplicate job name before create. For this purpose, we
     * want a job name that is not necessarily unique.
     *
     * @param jobName requested job name
     * @return qualified name to check
     */
    public String getRawTeamQualifiedJobName(String jobName) {
        List<Team> currentUserTeamsWithPermission = getCurrentUserTeamsWithPermission(Item.CREATE);
        if (!currentUserTeamsWithPermission.isEmpty()) {
            return getRawTeamQualifiedJobName(currentUserTeamsWithPermission.get(0), jobName);
        }
        return jobName;
    }

    /**
     * Called to check duplicate job name before create. For this purpose, we
     * want a job name that is not necessarily unique.
     *
     * @param team requested team
     * @param jobName requested job name
     * @return qualified name to check
     */
    public String getRawTeamQualifiedJobName(Team team, String jobName) {
        if (isPublicTeam(team)) {
            return jobName;
        }
        return team.getName() + TEAM_SEPARATOR + jobName;
    }

    /**
     *
     * @return the implicit team for the current user
     * @throws TeamNotFoundException
     */
    public Team findCurrentUserTeamForNewJob() throws TeamNotFoundException {
        // This will only find explicit team members with create permission
        List<Team> currentUserTeamsWithPermission = getCurrentUserTeamsWithPermission(Item.CREATE);
        if (!currentUserTeamsWithPermission.isEmpty()) {
            return currentUserTeamsWithPermission.get(0);
        }
        if (isCurrentUserSysAdmin()) {
            return publicTeam;
        }
        throw new TeamNotFoundException("User does not have Job create permission in any team");
    }
    
    public Team findCurrentUserTeamForNewView() throws TeamNotFoundException {
        // This will only find explicit team members with create permission
        List<Team> currentUserTeamsWithPermission = getCurrentUserTeamsWithPermission(View.CREATE);
        if (!currentUserTeamsWithPermission.isEmpty()) {
            return currentUserTeamsWithPermission.get(0);
        }
        if (isCurrentUserSysAdmin()) {
            return publicTeam;
        }
        throw new TeamNotFoundException("User does not have View create permission in any team");
    }
    
    public Team findCurrentUserTeamForNewNode() throws TeamNotFoundException {
        // This will only find explicit team members with create permission
        List<Team> currentUserTeamsWithPermission = getCurrentUserTeamsWithPermission(Computer.CREATE);
        if (!currentUserTeamsWithPermission.isEmpty()) {
            return currentUserTeamsWithPermission.get(0);
        }
        if (isCurrentUserSysAdmin()) {
            return publicTeam;
        }
        throw new TeamNotFoundException("User does not have Node create permission in any team");
    }
    
    public String getCurrentUserPrimaryView() {
        List<Team> currentUserTeams = findCurrentUserTeams();
        if (!currentUserTeams.isEmpty()) {
            Team team = currentUserTeams.get(0);
            return team.getPrimaryView();
        }
        return null;
    }

    /**
     * Get the current user team qualified Id for the job name
     *
     * @param team
     * @param jobName
     * @return String, Team qualified Job ID
     */
    public String getTeamQualifiedJobName(Team team, String jobName) {
        String teamName = team.getName();
        StringBuilder sb = Team.PUBLIC_TEAM_NAME.equals(teamName)
                ? new StringBuilder(jobName)
                : new StringBuilder(teamName + TEAM_SEPARATOR + jobName);
        // Make sure the name is unique
        Hudson h = Hudson.getInstance();
        int postfix = 2;
        int baseLength = sb.length();
        while (true) {
            String qualifiedName = sb.toString();
            if (findJobOwnerTeam(qualifiedName) == null) {
                break;
            }
            sb.setLength(baseLength);
            sb.append("_" + postfix++);
        }
        return sb.toString();
    }

    /**
     * Check that jobName is properly qualified for the team. The check fails
     * if:
     * <pre>
     *  - The team is public but the name is qualified (contains a '.')
     *  - The team is not public, but the name is not qualified
     *    by team name
     * </pre>
     *
     * @param team must not be null
     * @param jobName
     * @return true if check succeeds
     */
    public boolean isQualifiedJobName(Team team, String jobName) {
        if (isPublicTeam(team)) {
            return jobName.indexOf('.') < 0;
        }
        return jobName.startsWith(team.getName() + TEAM_SEPARATOR);
    }

    /**
     * Get the part of job name that is unique within the team. That is, given
     * <team-name>.<job-part> return job part if the job is already in
     * <team-name> or <team-name> is the current user team and team is not the
     * public team. Otherwise, return jobName.
     *
     * @param jobName
     * @return
     */
    public String getUnqualifiedJobName(String jobName) {
        Team team = findJobOwnerTeam(jobName);
        if (team == null) {
            if (!findCurrentUserTeams().isEmpty()) {
                team = findCurrentUserTeams().get(0);
            }
        }
        if (team != null) {
            return getUnqualifiedJobName(team, jobName);
        }
        return jobName;
    }

    /**
     * The Folder where all the jobs of the team to which this jobName belongs
     * to are stored.
     *
     * <p>
     * This method should be called to determine the jobs folder whether or not
     * team management is enabled, as team manager alone knows where team jobs
     * are.
     *
     * @param jobName
     * @return File, team jobs folder
     */
    public File getRootFolderForJob(String jobName) {
        Team team = findJobOwnerTeam(jobName);
        // May be just created job, get the job folder from the first 
        // team the current user or user role has create permission

        if ((team == null) && isTeamManagementEnabled()) {
            if (getTeamAwareSecurityRealm() != null) {
                team = getTeamAwareSecurityRealm().GetCurrentUserTeam();
            } else {
                List<Team> currentUserTeamsWithPermission = getCurrentUserTeamsWithPermission(Item.CREATE);
                if (!currentUserTeamsWithPermission.isEmpty()) {
                    team = currentUserTeamsWithPermission.get(0);
                }
            }
        }
        if (team != null) {
            if (isPublicTeam(team)) {
                return new File(team.getJobsFolder(hudsonHomeDir), jobName);
            } else {
                return new File(team.getJobsFolder(teamsFolder), jobName);
            }
        } else {
            // May be just created by sys admin who does not belong to any team
            return new File(publicTeam.getJobsFolder(hudsonHomeDir), jobName);
        }
    }

    /**
     * Get the root folders of all the jobs known to this Team manager
     *
     * @return
     */
    public File[] getJobsRootFolders() {
        List<File> jobsRootFolders = new ArrayList<File>();
        for (Team team : teams) {
            if (isPublicTeam(team)) {
                jobsRootFolders.addAll(team.getJobsRootFolders(hudsonHomeDir));
            } else {
                jobsRootFolders.addAll(team.getJobsRootFolders(teamsFolder));
            }
        }
        return jobsRootFolders.toArray(new File[jobsRootFolders.size()]);
    }

    List<Team> getCurrentUserTeamsWithPermission(Permission permission) {

        //Check if we have to use TeamAwareSecurityRealm
        if (getTeamAwareSecurityRealm() != null) {
            return Arrays.asList(getTeamAwareSecurityRealm().GetCurrentUserTeam());
        }

        List<Team> userTeamsWithPermission = new ArrayList<Team>();
        Authentication authentication = HudsonSecurityManager.getAuthentication();
        List<Team> userTeams = findCurrentUserTeams();
        for (Team userTeam : userTeams) {
            TeamMember member = userTeam.findMember(authentication.getName());
            if ((member != null) && member.hasPermission(permission)) {
                userTeamsWithPermission.add(userTeam);
            }
        }
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            for (Team userTeam : userTeams) {
                TeamMember member = userTeam.findMember(ga.getAuthority());
                if ((member != null) && member.hasPermission(permission)) {
                    userTeamsWithPermission.add(userTeam);
                }
            }
        }
        return userTeamsWithPermission;
    }

    public static final String ADMIN = "Admin";

    /**
     * All team permissions in sorted order
     */
    public static String[] ALL_TEAM_PERMISSIONS = new String[]{
        ADMIN, // not a real permission, but needed to distinguish admins
        Item.BUILD.getName(),
        Item.CONFIGURE.getName(),
        Item.CREATE.getName(),
        Item.DELETE.getName(),
        Item.EXTENDED_READ.getName(),
        Item.READ.getName(),
        Item.WIPEOUT.getName(),
        Item.WORKSPACE.getName(),};

    // Used in org.cli.ListTeamsCommand
    public List<String> getCurrentUserVisibleTeams() {
        List<String> teams = (List<String>) getCurrentUserTeams();
        if (!teams.contains(Team.PUBLIC_TEAM_NAME)) {
            teams.add(Team.PUBLIC_TEAM_NAME);
        }
        return teams;
    }

    // Used by ListTeamsCommand
    public boolean isUserHasAccessToTeam(String user, String team) {
        if (Team.PUBLIC_TEAM_NAME.equals(team)) {
            return true;
        }
        List<Team> userTeams = findUserTeams(user);
        for (Team userTeam : userTeams) {
            if (team.equals(userTeam.getName())) {
                return true;
            }
        }
        return false;
    }

    // Used by ListTeamsCommand
    public Collection<String> getCurrentUserAdminUsers() {
        Set<String> adminUsers = new TreeSet<String>();
        Collection<String> adminTeams = getCurrentUserAdminTeams();
        for (String teamName : adminTeams) {
            try {
                Team team = findTeam(teamName);
                List<TeamMember> members = team.getMembers();
                for (TeamMember member : members) {
                    adminUsers.add(member.getName());
                }
            } catch (TeamNotFoundException ex) {
                ; // shouldn't happen
            }
        }
        return adminUsers;
    }

    // Used by ListTeamsCommand
    public String[] getUserTeamPermissions(String user, String teamName) throws TeamNotFoundException {
        Team team = findTeam(teamName);
        if (isSysAdmin(user)) {
            return ALL_TEAM_PERMISSIONS;
        }
        TeamMember member = team.findMember(user);
        if (member != null) {
            List<String> memberPermissions = member.getPermissions();
            if (team.isAdmin(user)) {
                // Add the pseudo-permission
                memberPermissions.add(0, ADMIN);
            }
            String[] permissions = memberPermissions.toArray(new String[memberPermissions.size()]);
            Arrays.sort(permissions);
            return permissions;
        } else if (Team.PUBLIC_TEAM_NAME.equals(teamName)) {
            // Even anonymous can read
            return new String[]{Item.READ.getName()};
        }
        return new String[0];
    }

    // Used in org.cli.ListTeamsCommand?
    public String[] getCurrentUserTeamPermissions(String teamName) throws TeamNotFoundException {
        return getUserTeamPermissions(getCurrentUser(), teamName);
    }

    public boolean canNodeExecuteJob(String nodeName, String jobName) {
        Team nodeTeam = findNodeOwnerTeam(nodeName);
        Team jobTeam = findJobOwnerTeam(jobName);
        if ((nodeTeam != null) && (jobTeam != null)) {
            //If the node belongs to public then all teams can build
            //their job, unless the team admin disables it
            if (Team.PUBLIC_TEAM_NAME.equals(nodeTeam.getName()) && jobTeam.isVisibleNodeEnabled(nodeName)) {
                return true;
            }
            if (nodeTeam == jobTeam) {
                return true; 
            } else {
                TeamNode teamNode = nodeTeam.findNode(nodeName);
                if (teamNode != null) {
                    return teamNode.isVisible(jobTeam.getName()) && jobTeam.isVisibleNodeEnabled(nodeName);
                }
            }
        }
        return false;
    }

    public static class TeamNotFoundException extends Exception {

        public TeamNotFoundException(String teamName) {
            super("Team " + teamName + " does not exist.");
        }
    }

    public static class TeamAlreadyExistsException extends Exception {

        public TeamAlreadyExistsException(String teamName) {
            super("Team " + teamName + " already exists.");
        }
    }

    void setUseBulkSaveFlag(boolean flag) {
        useBulkSaveFlag = flag;
    }

    Team getPublicTeam() {
        return publicTeam;
    }

    /**
     * The file where the teams settings are saved.
     */
    private XmlFile getConfigFile() {
        return new XmlFile(xstream, new File(teamsFolder, teamsConfigFileName));
    }
    // This is purely fo unit test. Since Hudson is not fully loaded during
    // test BulkChange saving mode is not available
    private transient boolean useBulkSaveFlag = true;

    /**
     * Load the settings from the configuration file
     */
    private void load() {
        XmlFile config = getConfigFile();
        try {
            if (config.exists()) {
                config.unmarshal(this);
            }
        } catch (IOException e) {
            logger.error("Failed to load " + config, e);
        }
    }

    private void ensureCustomFolders() {
        // NB: It would be best to clean up jobs at any of the logger calls below
        // but we're in the TeamManager constructor so it can't be called from
        // Team to save teams.xml. These end cases should be rare.
        for (Team team : teams) {
            String customFolderName = team.getCustomFolderName();
            if (customFolderName != null && customFolderName.trim().length() > 0) {
                File jobsDir = team.getJobsFolder(teamsFolder);
                // In 3.1.0 there was no child jobs folder
                if (!jobsDir.exists()) {
                    List<TeamJob> jobs = team.getJobs();
                    if (!jobs.isEmpty()) {
                        // move the jobs to jobs folder
                        if (jobsDir.mkdirs()) {
                            for (TeamJob job : jobs) {
                                File teamDir = team.getTeamFolder(teamsFolder);
                                File oldJobDir = new File(teamDir, job.getId());
                                if (oldJobDir.exists() && oldJobDir.isDirectory()) {
                                    File newJobDir = new File(jobsDir, job.getId());
                                    try {
                                        Util.moveDirectory(oldJobDir, newJobDir);
                                    } catch (InterruptedException e) {
                                        logger.error("Failed to move " + oldJobDir.getAbsolutePath());
                                    }
                                } else {
                                    logger.error("Job folder not found " + oldJobDir.getAbsolutePath());
                                }
                            }
                        } else {
                            logger.error("Can't create " + jobsDir.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private void ensurePublicTeam() {
        publicTeam = new PublicTeam(this);
        try {
            Team team = findTeam(PublicTeam.PUBLIC_TEAM_NAME);
            teams.remove(team);
        } catch (TeamNotFoundException ex) {
            // It's ok, we are going to remove it any way
        }
        try {
            publicTeam.loadExistingJobs(hudsonHomeDir);
            Hudson hudson = Hudson.getInstance();
            //Null during initial setup 
            if (hudson != null) {
                for (View view : hudson.getAllViews()) {
                    TeamView teamView = new TeamView(view.getViewName());
                    if (view instanceof AllView) {
                        teamView.setMoveAllowed(false);
                        publicTeam.addView(teamView);
                    } else if (findViewOwnerTeam(view.getViewName()) == null) {
                        publicTeam.addView(teamView);
                    }
                }
                for (Computer node : hudson.getAllComputers()) {
                    TeamNode teamNode = new TeamNode(node.getName());
                    if (node instanceof Hudson.MasterComputer) {
                        //Master node does not have a name!!
                        teamNode.setId("Master");
                        teamNode.setMoveAllowed(false);
                        publicTeam.addNode(teamNode);
                    } else if (findNodeOwnerTeam(node.getName()) == null) {
                        publicTeam.addNode(teamNode);
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to load existing jobs", ex);
        }
        teams.add(publicTeam);
    }

    boolean isPublicTeam(Team team) {
        return Team.PUBLIC_TEAM_NAME.equals(team.getName());
    }

    private void initializeXstream() {
        xstream.alias("teamManager", TeamManager.class);
        xstream.alias("team", Team.class);
        xstream.alias("publicTeam", PublicTeam.class);
    }

    public static class ConverterImpl implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == TeamManager.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            TeamManager teamManager = (TeamManager) source;
            for (String sid : teamManager.sysAdmins) {
                writer.startNode("sysAdmin");
                writer.setValue(sid);
                writer.endNode();
            }
            for (Team team : teamManager.teams) {
                writer.startNode("team");
                context.convertAnother(team);
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            TeamManager teamManager = (TeamManager) uc.currentObject();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("sysAdmin".equals(reader.getNodeName())) {
                    teamManager.sysAdmins.add(reader.getValue());
                } else if ("team".equals(reader.getNodeName())) {
                    Team team = (Team) uc.convertAnother(teamManager, Team.class);
                    teamManager.teams.add(team);
                }
                reader.moveUp();
            }
            return teamManager;
        }
    }
}
