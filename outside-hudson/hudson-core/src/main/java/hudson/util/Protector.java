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
 *    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package hudson.util;

import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

/**
 * Encrypt/decrypt data by using a "session" key that only lasts for the
 * duration of the server instance.
 *
 * @author Kohsuke Kawaguchi
 * @see Scrambler
 * @since 1.162
 */
public class Protector {

    private static final String ALGORITHM = "DES";
    private static final String MAGIC = ":::";

    public static String protect(String secret) {
        try {
            Cipher cipher = Secret.getCipher(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, DES_KEY);
            return new String(Base64.encodeBase64(cipher.doFinal((secret + MAGIC).getBytes("UTF-8"))));
        } catch (GeneralSecurityException e) {
            throw new Error(e); // impossible
        } catch (UnsupportedEncodingException e) {
            throw new Error(e); // impossible
        }
    }

    /**
     * Returns null if fails to decrypt properly.
     */
    public static String unprotect(String data) {
        if (data == null) {
            return null;
        }
        try {
            Cipher cipher = Secret.getCipher(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, DES_KEY);
            String plainText = new String(cipher.doFinal(Base64.decodeBase64(data)), "UTF-8");
            if (plainText.endsWith(MAGIC)) {
                return plainText.substring(0, plainText.length() - 3);
            }
            return null;
        } catch (GeneralSecurityException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e); // impossible
        } catch (IOException e) {
            return null;
        }
    }
    private static final SecretKey DES_KEY;

    static {
        try {
            DES_KEY = KeyGenerator.getInstance(ALGORITHM).generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }
}
