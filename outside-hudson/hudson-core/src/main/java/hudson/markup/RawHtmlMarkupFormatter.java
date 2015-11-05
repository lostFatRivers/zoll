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

package hudson.markup;

import com.google.common.base.Throwables;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Writer;
import org.owasp.html.Handler;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamRenderer;

/**
 * {@link MarkupFormatter} that treats the input as the raw html. This is the
 * backward compatible behaviour.
 *
 * @author Kohsuke Kawaguchi
 */
public class RawHtmlMarkupFormatter extends MarkupFormatter {

    @DataBoundConstructor
    public RawHtmlMarkupFormatter() {
    }

    @Override
    public void translate(String markup, Writer output) throws IOException {
        HtmlStreamRenderer renderer = HtmlStreamRenderer.create(
                output,
                // Receives notifications on a failure to write to the output.
                new Handler<IOException>() {
                    public void handle(IOException ex) {
                        Throwables.propagate(ex);  // System.out suppresses IOExceptions
                    }
                },
                // Our HTML parser is very lenient, but this receives notifications on
                // truly bizarre inputs.
                new Handler<String>() {
                    public void handle(String x) {
                        throw new Error(x);
                    }
                }
        );
        // Use the policy defined above to sanitize the HTML.
        HtmlStreamEventReceiver receiver = EbayPolicyExample.getEnforceTableNestingReceiver(renderer);
        HtmlSanitizer.sanitize(markup, EbayPolicyExample.POLICY_DEFINITION.apply(receiver));
    }

    @Extension
    public static class DescriptorImpl extends MarkupFormatterDescriptor {

        @Override
        public String getDisplayName() {
            return "Raw HTML";
        }
    }
    public static MarkupFormatter INSTANCE = new RawHtmlMarkupFormatter();
}
