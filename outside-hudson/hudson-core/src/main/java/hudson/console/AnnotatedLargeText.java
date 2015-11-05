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

package hudson.console;

import hudson.model.Hudson;
import hudson.remoting.ObjectInputStreamEx;
import hudson.util.IOException2;
import hudson.util.IOUtils;
import hudson.util.Secret;
import hudson.util.TimeUnit2;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.ByteBuffer;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.Math.abs;
import org.apache.commons.codec.binary.Base64;

/**
 * Extension to {@link LargeText} that handles annotations by
 * {@link ConsoleAnnotator}.
 *
 * <p> In addition to run each line through
 * {@link ConsoleAnnotationOutputStream} for adding markup, this class persists
 * {@link ConsoleAnnotator} into a byte sequence and send it to the client as an
 * HTTP header. The client JavaScript sends it back next time it fetches the
 * following output.
 *
 * <p> The serialized {@link ConsoleAnnotator} is encrypted to avoid malicious
 * clients from instantiating arbitrary {@link ConsoleAnnotator}s.
 *
 * @param <T> Context type.
 * @author Kohsuke Kawaguchi
 * @since 1.349
 */
public class AnnotatedLargeText<T> extends LargeText {

    /**
     * Can be null.
     */
    private T context;

    public AnnotatedLargeText(File file, Charset charset, boolean completed, T context) {
        super(file, charset, completed);
        this.context = context;
    }

    public AnnotatedLargeText(ByteBuffer memory, Charset charset, boolean completed, T context) {
        super(memory, charset, completed);
        this.context = context;
    }

    public void doProgressiveHtml(StaplerRequest req, StaplerResponse rsp) throws IOException {
        req.setAttribute("html", true);
        doProgressText(req, rsp);
    }

    /**
     * Aliasing what I think was a wrong name in {@link LargeText}
     */
    public void doProgressiveText(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doProgressText(req, rsp);
    }

    /**
     * For reusing code between text/html and text/plain, we run them both
     * through the same code path and use this request attribute to
     * differentiate.
     */
    private boolean isHtml() {
        return Stapler.getCurrentRequest().getAttribute("html") != null;
    }

    @Override
    protected void setContentType(StaplerResponse rsp) {
        rsp.setContentType(isHtml() ? "text/html;charset=UTF-8" : "text/plain;charset=UTF-8");
    }

    private ConsoleAnnotator createAnnotator(StaplerRequest req) throws IOException {
        ObjectInputStream ois = null;
        try {
            String base64 = req != null ? req.getHeader("X-ConsoleAnnotator") : null;
            if (base64 != null) {
                Cipher sym = Secret.getCipher("AES");
                sym.init(Cipher.DECRYPT_MODE, Hudson.getInstance().getSecretKeyAsAES128());

                ois = new ObjectInputStreamEx(new GZIPInputStream(
                        new CipherInputStream(new ByteArrayInputStream(Base64.decodeBase64(base64)), sym)),
                        Hudson.getInstance().pluginManager.uberClassLoader);
                long timestamp = ois.readLong();
                if (TimeUnit2.HOURS.toMillis(1) > abs(System.currentTimeMillis() - timestamp)) // don't deserialize something too old to prevent a replay attack
                {
                    return (ConsoleAnnotator) ois.readObject();
                }
            }
        } catch (GeneralSecurityException e) {
            throw new IOException2(e);
        } catch (ClassNotFoundException e) {
            throw new IOException2(e);
        } finally {
            IOUtils.closeQuietly(ois);
        }
        // start from scratch
        return ConsoleAnnotator.initial(context == null ? null : context.getClass());
    }

    @Override
    public long writeLogTo(long start, Writer w) throws IOException {
        if (isHtml()) {
            return writeHtmlTo(start, w);
        } else {
            return super.writeLogTo(start, w);
        }
    }

    @Override
    public long writeLogTo(long start, OutputStream out) throws IOException {
        return super.writeLogTo(start, new PlainTextConsoleOutputStream(out));
    }

    public long writeHtmlTo(long start, Writer w) throws IOException {
        ConsoleAnnotationOutputStream caw = new ConsoleAnnotationOutputStream(
                w, createAnnotator(Stapler.getCurrentRequest()), context, charset);
        long r = super.writeLogTo(start, caw);

        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Cipher sym = Secret.getCipher("AES");
            sym.init(Cipher.ENCRYPT_MODE, Hudson.getInstance().getSecretKeyAsAES128());
            try {
                oos = new ObjectOutputStream(new GZIPOutputStream(new CipherOutputStream(baos, sym)));
                oos.writeLong(System.currentTimeMillis()); // send timestamp to prevent a replay attack
                oos.writeObject(caw.getConsoleAnnotator());
            } finally {
                IOUtils.closeQuietly(oos);
            }
            StaplerResponse rsp = Stapler.getCurrentResponse();
            if (rsp != null) {
                rsp.setHeader("X-ConsoleAnnotator", new String(Base64.encodeBase64(baos.toByteArray())));
            }
        } catch (GeneralSecurityException e) {
            throw new IOException2(e);
        }
        return r;
    }
}
