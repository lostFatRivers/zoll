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
 *    Michael B. Donohue, Seiji Sogabe, Winston Prakash
 *
 *******************************************************************************/ 

package hudson.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hudson.console.HyperlinkNote;
import hudson.diagnosis.OldDataMonitor;
import hudson.util.XStream2;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.hudson.security.HudsonSecurityManager;

/**
 * Cause object base class. This class hierarchy is used to keep track of why a
 * given build was started. This object encapsulates the UI rendering of the
 * cause, as well as providing more useful information in respective subypes.
 *
 * The Cause object is connected to a build via the {@link CauseAction} object.
 *
 * <h2>Views</h2> <dl> <dt>description.jelly <dd>Renders the cause to HTML. By
 * default, it puts the short description. </dl>
 *
 * @author Michael Donohue
 * @see Run#getCauses()
 * @see Queue.Item#getCauses()
 */
@ExportedBean
public abstract class Cause {

    /**
     * One-line human-readable text of the cause.
     *
     * <p> By default, this method is used to render HTML as well.
     */
    @Exported(visibility = 3)
    public abstract String getShortDescription();

    /**
     * Called when the cause is registered to {@link AbstractBuild}.
     *
     * @param build never null
     * @since 1.376
     */
    public void onAddedTo(AbstractBuild build) {
    }

    /**
     * Report a line to the listener about this cause.
     *
     * @since 1.362
     */
    public void print(TaskListener listener) {
        listener.getLogger().println(getShortDescription());
    }

    /**
     * Fall back implementation when no other type is available.
     *
     * @deprecated since 2009-02-08
     */
    public static class LegacyCodeCause extends Cause {

        private StackTraceElement[] stackTrace;

        public LegacyCodeCause() {
            stackTrace = new Exception().getStackTrace();
        }

        @Override
        public String getShortDescription() {
            return Messages.Cause_LegacyCodeCause_ShortDescription();
        }
    }

    /**
     * A build is triggered by the completion of another build (AKA upstream
     * build.)
     */
    public static class UpstreamCause extends Cause {

        private String upstreamProject, upstreamUrl;
        private int upstreamBuild;
        /**
         * @deprecated since 2009-02-28
         */
        @Deprecated
        private transient Cause upstreamCause;
        private List<Cause> upstreamCauses;

        /**
         * @deprecated since 2009-02-28
         */
        // for backward bytecode compatibility
        public UpstreamCause(AbstractBuild<?, ?> up) {
            this((Run<?, ?>) up);
        }

        public UpstreamCause(Run<?, ?> up) {
            upstreamBuild = up.getNumber();
            upstreamProject = up.getParent().getFullName();
            upstreamUrl = up.getParent().getUrl();
            upstreamCauses = new ArrayList<Cause>(up.getCauses());
        }

        /**
         * Returns true if this cause points to a build in the specified job.
         */
        public boolean pointsTo(Job<?, ?> j) {
            return j.getFullName().equals(upstreamProject);
        }

        /**
         * Returns true if this cause points to the specified build.
         */
        public boolean pointsTo(Run<?, ?> r) {
            return r.getNumber() == upstreamBuild && pointsTo(r.getParent());
        }

        @Exported(visibility = 3)
        public String getUpstreamProject() {
            return upstreamProject;
        }

        @Exported(visibility = 3)
        public int getUpstreamBuild() {
            return upstreamBuild;
        }

        @Exported(visibility = 3)
        public String getUpstreamUrl() {
            return upstreamUrl;
        }

        @Override
        public String getShortDescription() {
            return Messages.Cause_UpstreamCause_ShortDescription(upstreamProject, upstreamBuild);
        }

        @Override
        public void print(TaskListener listener) {
            listener.getLogger().println(
                    Messages.Cause_UpstreamCause_ShortDescription(
                    HyperlinkNote.encodeTo('/' + upstreamUrl, upstreamProject),
                    HyperlinkNote.encodeTo('/' + upstreamUrl + upstreamBuild, Integer.toString(upstreamBuild))));
        }

        public static class ConverterImpl extends XStream2.PassthruConverter<UpstreamCause> {

            public ConverterImpl(XStream2 xstream) {
                super(xstream);
            }

            @Override
            protected void callback(UpstreamCause uc, UnmarshallingContext context) {
                if (uc.upstreamCause != null) {
                    if (uc.upstreamCauses == null) {
                        uc.upstreamCauses = new ArrayList<Cause>();
                    }
                    uc.upstreamCauses.add(uc.upstreamCause);
                    uc.upstreamCause = null;
                    OldDataMonitor.report(context, "1.288");
                }
            }
        }
    }

    /**
     * A build is started by an user action.
     */
    public static class UserCause extends Cause {

        private String authenticationName;

        public UserCause() {
            this.authenticationName = HudsonSecurityManager.getAuthentication().getName();
        }

        @Exported(visibility = 3)
        public String getUserName() {
            User u = User.get(authenticationName, false);
            return u != null ? u.getDisplayName() : authenticationName;
        }
        
        @Exported(visibility = 3)
        public String getUserId() {
            User u = User.get(authenticationName, false);
            return u != null ? u.getId() : authenticationName;
        }

        @Override
        public String getShortDescription() {
            return Messages.Cause_UserCause_ShortDescription(authenticationName);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof UserCause && Arrays.equals(new Object[]{authenticationName},
                    new Object[]{((UserCause) o).authenticationName});
        }

        @Override
        public int hashCode() {
            return 295 + (this.authenticationName != null ? this.authenticationName.hashCode() : 0);
        }
    }

    public static class RemoteCause extends Cause {

        private String addr;
        private String note;

        public RemoteCause(String host, String note) {
            this.addr = host;
            this.note = note;
        }

        @Override
        public String getShortDescription() {
            if (note != null) {
                try {
                    note =  Hudson.getInstance().getMarkupFormatter().translate(note);
                    return Messages.Cause_RemoteCause_ShortDescriptionWithNote(addr, note);
                } catch (IOException ex) {
                    Logger.getLogger(Cause.class.getName()).log(Level.INFO, "Failed to Markup filter remote cause " + note , ex);
                    return "Warning: Failed to Markup filter remote cause";
                }
            } else {
                return Messages.Cause_RemoteCause_ShortDescription(addr);
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RemoteCause && Arrays.equals(new Object[]{addr, note},
                    new Object[]{((RemoteCause) o).addr, ((RemoteCause) o).note});
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 83 * hash + (this.addr != null ? this.addr.hashCode() : 0);
            hash = 83 * hash + (this.note != null ? this.note.hashCode() : 0);
            return hash;
        }
    }
}
