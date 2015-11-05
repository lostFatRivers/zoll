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

import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleProjectMock;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.hudson.security.team.TeamManager.TeamNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

/**
 * Test class for TeamBasedACL
 *
 * @author Winston Prakash
 */
public class TeamJobACLTest {

    private Permission configurePermission = Item.CONFIGURE;
    private Permission readPermission = Item.READ;
    private File homeDir = FileUtils.getTempDirectory();
    private File teamsFolder = new File(homeDir, "teams");
    private final String teamsConfigFileName = "teams.xml";
    private File teamsStore = new File(teamsFolder, teamsConfigFileName);
    private TeamManager teamManager;

    @Before
    public void setUp() {
        if (teamsStore.exists()) {
            teamsStore.delete();
        }
        teamManager = new TeamManager(homeDir);
        teamManager.setUseBulkSaveFlag(false);
    }

    @After
    public void tearDown() {
        if (teamsStore.exists()) {
            teamsStore.delete();
        }
    }

    @Test
    public void testJobPermission() throws IOException, TeamManager.TeamAlreadyExistsException, TeamNotFoundException {
        String teamName = "team1";
        Team team = teamManager.createTeam(teamName);
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");

        TeamMember newMember = new TeamMember();
        newMember.setName("Paul");
        newMember.addPermission(Item.CONFIGURE);
        team.addMember(newMember);
        
        teamManager.addJobToUserTeam("Paul", freeStyleJob.getName());


        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertTrue("Paul is a team member with Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());

        Sid sid2 = new PrincipalSid("Chris");
        Assert.assertNull("Chris is not a team member and should not have Job CONFIGURE permission", teamBasedACL.hasPermission(sid2, configurePermission));
        Assert.assertNull("Chris is not a team member and should not have Job READ permission", teamBasedACL.hasPermission(sid2, readPermission));

    }

    @Test
    public void testPublicJobPermission() throws IOException {
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");

        teamManager.getPublicTeam().addJob(new TeamJob(freeStyleJob.getName()));

        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertNull("Paul is not a SysAdmin and should not have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission));
        Assert.assertTrue("Paul should have pubic Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

        teamManager.addSysAdmin("Paul");
        Assert.assertTrue("Paul is a SysAdmin and should have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
    }

    @Test
    public void testAnonymousPublicJobPermission() throws IOException {
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");

        teamManager.getPublicTeam().addJob(new TeamJob(freeStyleJob.getName()));


        Sid sid = ACL.ANONYMOUS;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertNull("Anonymous should not have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission));
        Assert.assertTrue("Anonymous should have public Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }

    @Test
    public void testEveryonePublicJobPermission() throws IOException {
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");

        teamManager.getPublicTeam().addJob(new TeamJob(freeStyleJob.getName()));
        Sid sid = ACL.EVERYONE;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertNull("Every one should not have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission));
        Assert.assertTrue("Every one should have piublic Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }

    @Test
    public void testJobVisibility() throws IOException, TeamManager.TeamAlreadyExistsException {
        String teamName = "team1";
        Team team = teamManager.createTeam(teamName);
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        TeamJob teamJob = new TeamJob(freeStyleJob.getName());
        teamJob.addVisibility("public");
        team.addJob(teamJob);

        Sid sid = ACL.ANONYMOUS;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertTrue("Anonymous should have testJob READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

        teamJob.removeVisibility("public");
        Assert.assertNull("Anonymous should not have testJob READ permission", teamBasedACL.hasPermission(sid, readPermission));

        String teamName2 = "team2";
        Team team2 = teamManager.createTeam(teamName2);
        TeamMember newMember = new TeamMember();
        newMember.setName("Chris");
        newMember.addPermission(Item.CONFIGURE);
        team2.addMember(newMember);

        teamJob.addVisibility(team2.getName());

        Sid sid2 = new PrincipalSid("Chris");
        Assert.assertNull("Chris should not have Job CONFIGURE permission", teamBasedACL.hasPermission(sid2, configurePermission));
        Assert.assertTrue("Chris should have testJob READ permission", teamBasedACL.hasPermission(sid2, readPermission).booleanValue());

    }
}