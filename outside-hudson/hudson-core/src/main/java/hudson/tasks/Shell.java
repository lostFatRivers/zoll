/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Kohsuke Kawaguchi, Jene Jasper, Yahoo! Inc., Nikita Levyankov
 *
 *
 *******************************************************************************/ 

package hudson.tasks;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Executes a series of commands by using a shell.
 *
 * @author Kohsuke Kawaguchi
 */
public class Shell extends CommandInterpreter {

    private static final Logger LOGGER = Logger.getLogger(Shell.class.getName());
    
    public Shell(String command){
        this(command, false, "");
    }
    @DataBoundConstructor
    public Shell(String command, boolean disabled, String description) {
        super(fixCrLf(command));
        this.setDisabled(disabled); 
        setDescription(description);
    }

    /**
     * Fix CR/LF and always make it Unix style.
     */
    private static String fixCrLf(String s) {
        // eliminate CR
        int idx;
        if (null != s) { //avoid potential NullPointerException if command is null.
            while ((idx = s.indexOf("\r\n")) != -1) {
                s = s.substring(0, idx) + s.substring(idx + 1);
            }
        }
        return s;
    }

    /**
     * Older versions of bash have a bug where non-ASCII on the first line makes
     * the shell think the file is a binary file and not a script. Adding a
     * leading line feed works around this problem.
     */
    private static String addCrForNonASCII(String s) {
        if (!s.startsWith("#!")) {
            if (s.indexOf('\n') != 0) {
                return "\n" + s;
            }
        }

        return s;
    }

    public String[] buildCommandLine(FilePath script) {
        if (command.startsWith("#!")) {
            // interpreter override
            int end = command.indexOf('\n');
            if (end < 0) {
                end = command.length();
            }
            List<String> args = new ArrayList<String>();
            args.addAll(Arrays.asList(Util.tokenize(command.substring(0, end).trim())));
            args.add(script.getRemote());
            args.set(0, args.get(0).substring(2));   // trim off "#!"
            return args.toArray(new String[args.size()]);
        } else {
            return new String[]{getDescriptor().getShellOrDefault(), "-xe", script.getRemote()};
        }
    }

    protected String getContents() {
        return addCrForNonASCII(fixCrLf(command));
    }

    protected String getFileExtension() {
        return ".sh";
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * Shell executable, or null to default.
         */
        private String shell;

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getShell() {
            return shell;
        }

        public String getShellOrDefault() {
            if (shell == null) {
                return Functions.isWindows() ? "sh" : "/bin/sh";
            }
            return shell;
        }
        
        public String getShellOrDefault(VirtualChannel channel) {
            if (shell != null) 
                return shell;

            String interpreter = null;
            try {
                interpreter = channel.call(new Shellinterpreter());
            } catch (IOException e) {
                LOGGER.warning(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.warning(e.getMessage());
            }
            if (interpreter == null) {
                interpreter = getShellOrDefault();
            }

            return interpreter;
        }

        public void setShell(String shell) {
            this.shell = Util.fixEmptyAndTrim(shell);
            save();
        }

        public String getDisplayName() {
            return Messages.Shell_DisplayName();
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
            return new Shell(data.getString("command"), data.getBoolean("disabled"), data.getString("description"));
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject data) {
            setShell(req.getParameter("shell"));
            return true;
        }

        /**
         * Check the existence of sh in the given location.
         */
        public FormValidation doCheck(@QueryParameter String value) {
            // Executable requires admin permission
            return FormValidation.validateExecutable(value);
        }
        
        private static final class Shellinterpreter implements Callable<String, IOException> {

            private static final long serialVersionUID = 1L;

            public String call() throws IOException {
                return Functions.isWindows() ? "sh" : "/bin/sh";
            }
        }
    }
}
