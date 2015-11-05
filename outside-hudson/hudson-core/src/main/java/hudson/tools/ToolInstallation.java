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
 *    Tom Huybrechts
 *
 *
 *******************************************************************************/ 

package hudson.tools;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.AbstractDescribableImpl;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.util.DescribableList;
import hudson.util.XStream2;
import hudson.util.XStreamSerializable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Formalization of a tool installed in nodes used for builds (examples include
 * things like JDKs, Ants, Mavens, etc..)
 *
 * <p> You can define such a concept in your plugin entirely on your own,
 * without extending from this class, but choosing this class as a base class
 * has several benefits:
 *
 * <ul> <li>Hudson allows admins to specify different locations for tools on
 * some slaves. For example, JDK on the master might be on /usr/local/java but
 * on a Windows slave it could be at c:\Program Files\Java <li>Hudson can verify
 * the existence of tools and provide warnings and diagnostics for admins. (TBD)
 * <li>Hudson can perform automatic installations for users. (TBD) </ul>
 *
 * <p> Implementations of this class are strongly encouraged to also implement
 * {@link NodeSpecific} (by using {@link #translateFor(Node, TaskListener)}) and
 * {@link EnvironmentSpecific} (by using {@link EnvVars#expand(String)}.)
 *
 * <p> To contribute an extension point, put {@link Extension} on your
 * {@link ToolDescriptor} class.
 *
 * @author huybrechts
 * @since 1.286
 */
public abstract class ToolInstallation extends AbstractDescribableImpl<ToolInstallation> implements Serializable, ExtensionPoint {

    private final String name;
    private /*almost final*/ String home;
    /**
     * {@link ToolProperty}s that are associated with this tool.
     */
    @XStreamSerializable
    private transient /*almost final*/ DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = new DescribableList<ToolProperty<?>, ToolPropertyDescriptor>(Saveable.NOOP);

    /**
     * @deprecated as of 1.302. Use
     * {@link #ToolInstallation(String, String, List)}
     */
    public ToolInstallation(String name, String home) {
        this.name = name;
        this.home = home;
    }

    public ToolInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        this.name = name;
        this.home = home;
        if (properties != null) {
            try {
                this.properties.replaceBy(properties);
                for (ToolProperty<?> p : properties) {
                    _setTool(p, this);
                }
            } catch (IOException e) {
                throw new AssertionError(e); // no Saveable, so can't happen
            }
        }
    }

    // helper function necessary to avoid a warning
    private <T extends ToolInstallation> void _setTool(ToolProperty<T> prop, ToolInstallation t) {
        prop.setTool(prop.type().cast(t));
    }

    /**
     * Gets the human readable name that identifies this tool among other
     * {@link ToolInstallation}s of the same kind.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the home directory of this tool.
     *
     * The path can be in Unix format as well as in Windows format. Must be
     * absolute.
     */
    public String getHome() {
        return home;
    }

    public DescribableList<ToolProperty<?>, ToolPropertyDescriptor> getProperties() {
        assert properties != null;
        return properties;
    }

    /**
     * Finds a tool on a node. Checks if the location of the tool is overridden
     * for the given node, and if so, return the node-specific home directory.
     * Also checks available {@link ToolLocationTranslator}s. Otherwise returns
     * {@code installation.getHome()}.
     *
     * <p> This is the core logic behind
     * {@link NodeSpecific#forNode(Node, TaskListener)} for
     * {@link ToolInstallation}, and meant to be used by the {@code forNode}
     * implementations.
     *
     * @return never null.
     */
    @SuppressWarnings("deprecation")
    protected String translateFor(Node node, TaskListener log) throws IOException, InterruptedException {
        return ToolLocationNodeProperty.getToolHome(node, this, log);
    }

    /**
     * Invoked by XStream when this object is read into memory.
     */
    public Object readResolve() {
        if (properties == null) {
            properties = new DescribableList<ToolProperty<?>, ToolPropertyDescriptor>(Saveable.NOOP);
        }
        for (ToolProperty<?> p : properties) {
            _setTool(p, this);
        }
        return this;
    }

    /**
     * Subclasses can extend this for data migration from old field storing home
     * directory.
     */
    protected static abstract class ToolConverter extends XStream2.PassthruConverter<ToolInstallation> {

        public ToolConverter(XStream2 xstream) {
            super(xstream);
        }

        protected void callback(ToolInstallation obj, UnmarshallingContext context) {
            String s;
            if (obj.home == null && (s = oldHomeField(obj)) != null) {
                obj.home = s;
                OldDataMonitor.report(context, "1.286");
            }
        }

        protected abstract String oldHomeField(ToolInstallation obj);
    }

    /**
     * Returns all the registered {@link ToolDescriptor}s.
     */
    public static DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> all() {
        // use getDescriptorList and not getExtensionList to pick up legacy instances
        return Hudson.getInstance().<ToolInstallation, ToolDescriptor<?>>getDescriptorList(ToolInstallation.class);
    }
    private static final long serialVersionUID = 1L;
}
