/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Kohsuke Kawaguchi, Winston Prakash, Peter Hayes, Tom Huybrechts
 *
 *******************************************************************************/ 

package hudson.security;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.Extension;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Run;
import hudson.model.View;
import hudson.util.FormValidation;
import hudson.util.RobustReflectionConverter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.security.acls.model.Sid;

/**
 * {@link JobProperty} to associate ACL for each project.
 *
 * <p> Once created (and initialized), this object becomes immutable.
 */
public class AuthorizationMatrixProperty extends JobProperty<Job<?, ?>> {

    private transient SidACL acl = new AclImpl();
    /**
     * List up all permissions that are granted.
     *
     * Strings are either the granted authority or the principal, which is not
     * distinguished.
     */
    private final Map<Permission, Set<String>> grantedPermissions = new HashMap<Permission, Set<String>>();
    private Set<String> sids = new HashSet<String>();

    private AuthorizationMatrixProperty() {
    }

    public AuthorizationMatrixProperty(Map<Permission, Set<String>> grantedPermissions) {
        // do a deep copy to be safe
        for (Entry<Permission, Set<String>> e : grantedPermissions.entrySet()) {
            this.grantedPermissions.put(e.getKey(), new HashSet<String>(e.getValue()));
        }
    }

    public Set<String> getGroups() {
        return sids;
    }

    /**
     * Returns all SIDs configured in this matrix, minus "anonymous"
     *
     * @return Always non-null.
     */
    public List<String> getAllSIDs() {
        Set<String> r = new HashSet<String>();
        for (Set<String> set : grantedPermissions.values()) {
            r.addAll(set);
        }
        r.remove("anonymous");

        String[] data = r.toArray(new String[r.size()]);
        Arrays.sort(data);
        return Arrays.asList(data);
    }

    /**
     * Returns all the (Permission,sid) pairs that are granted, in the multi-map
     * form.
     *
     * @return read-only. never null.
     */
    public Map<Permission, Set<String>> getGrantedPermissions() {
        return Collections.unmodifiableMap(grantedPermissions);
    }

    /**
     * Adds to {@link #grantedPermissions}. Use of this method should be limited
     * during construction, as this object itself is considered immutable once
     * populated.
     */
    protected void add(Permission p, String sid) {
        Set<String> set = grantedPermissions.get(p);
        if (set == null) {
            grantedPermissions.put(p, set = new HashSet<String>());
        }
        set.add(sid);
        sids.add(sid);
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            formData = formData.getJSONObject("useProjectSecurity");
            if (formData.isNullObject()) {
                return null;
            }

            AuthorizationMatrixProperty amp = new AuthorizationMatrixProperty();
            for (Map.Entry<String, Object> r : (Set<Map.Entry<String, Object>>) formData.getJSONObject("data").entrySet()) {
                String sid = r.getKey();
                if (r.getValue() instanceof JSONObject) {
                    for (Map.Entry<String, Boolean> e : (Set<Map.Entry<String, Boolean>>) ((JSONObject) r
                            .getValue()).entrySet()) {
                        if (e.getValue()) {
                            Permission p = Permission.fromId(e.getKey());
                            amp.add(p, sid);
                        }
                    }
                }
            }
            return amp;
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            // only applicable when ProjectMatrixAuthorizationStrategy is in charge
            return HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getAuthorizationStrategy() instanceof ProjectMatrixAuthorizationStrategy;
        }

        @Override
        public String getDisplayName() {
            return "Authorization Matrix";
        }

        public List<PermissionGroup> getAllGroups() {
            return Arrays.asList(PermissionGroup.get(Item.class), PermissionGroup.get(Run.class));
        }

        public boolean showPermission(Permission p) {
            // These three are only used by Team Authorization
            if (p == Computer.READ){
                return false;
            }
            if (p == Computer.CREATE){
                return false;
            }
            if (p == View.READ){
                return false;
            }
            return p.getEnabled() && p != Item.CREATE;
        }

        public FormValidation doCheckName(@AncestorInPath Job project, @QueryParameter String value) throws IOException, ServletException {
            return GlobalMatrixAuthorizationStrategy.DESCRIPTOR.doCheckName(value, project, AbstractProject.CONFIGURE);
        }
    }

    private final class AclImpl extends SidACL {

        protected Boolean hasPermission(Sid sid, Permission p) {
            if (AuthorizationMatrixProperty.this.hasPermission(toString(sid), p)) {
                return true;
            }
            return null;
        }
    }

    public SidACL getACL() {
        return acl;
    }

    /**
     * Checks if the given SID has the given permission.
     */
    public boolean hasPermission(String sid, Permission p) {
        for (; p != null; p = p.impliedBy) {
            Set<String> set = grantedPermissions.get(p);
            if (set != null && set.contains(sid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the permission is explicitly given, instead of implied through
     * {@link Permission#impliedBy}.
     */
    public boolean hasExplicitPermission(String sid, Permission p) {
        Set<String> set = grantedPermissions.get(p);
        return set != null && set.contains(sid);
    }

    /**
     * Works like {@link #add(Permission, String)} but takes both parameters
     * from a single string of the form <tt>PERMISSIONID:sid</tt>
     */
    private void add(String shortForm) {
        int idx = shortForm.indexOf(':');
        Permission p = Permission.fromId(shortForm.substring(0, idx));
        if (p == null) {
            throw new IllegalArgumentException("Failed to parse '" + shortForm + "' --- no such permission");
        }
        add(p, shortForm.substring(idx + 1));
    }

    /**
     * Persist {@link ProjectMatrixAuthorizationStrategy} as a list of IDs that
     * represent {@link ProjectMatrixAuthorizationStrategy#grantedPermissions}.
     */
    public static final class ConverterImpl implements Converter {

        public boolean canConvert(Class type) {
            return type == AuthorizationMatrixProperty.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            AuthorizationMatrixProperty amp = (AuthorizationMatrixProperty) source;

            for (Entry<Permission, Set<String>> e : amp.grantedPermissions
                    .entrySet()) {
                String p = e.getKey().getId();
                for (String sid : e.getValue()) {
                    writer.startNode("permission");
                    writer.setValue(p + ':' + sid);
                    writer.endNode();
                }
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                final UnmarshallingContext context) {
            AuthorizationMatrixProperty as = new AuthorizationMatrixProperty();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                try {
                    String prop = reader.getValue();
                    if (prop != null) {
                        if (prop.equals("useProjectSecurity")) {
                            // Do nothing, we no longer use it
                        } else {
                            as.add(prop);
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(AuthorizationMatrixProperty.class.getName())
                            .log(Level.WARNING, "Skipping a non-existent permission", ex);
                    RobustReflectionConverter.addErrorInContext(context, ex);
                }
                reader.moveUp();
            }

            if (GlobalMatrixAuthorizationStrategy.migrateHudson2324(as.grantedPermissions)) {
                OldDataMonitor.report(context, "1.301");
            }

            return as;
        }
    }
}
