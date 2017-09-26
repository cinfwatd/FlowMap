/*
  MIT License

  Copyright (c) 2017 Cinfwat Dogak

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

package me.dcii.flowmap.util;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import static me.dcii.flowmap.util.Constants.AES_BIT_LENGTH;
import static me.dcii.flowmap.util.Constants.KEYSTORE_PROVIDER;
import static me.dcii.flowmap.util.Constants.RSA_BIT_LENGTH;
import static me.dcii.flowmap.util.Constants.RSA_KEY_ALIAS;

/**
 * Provides methods for accessing and generating keys from the {@link java.security.KeyStore}.
 * Used to encrypt the Realm store.
 *
 * @author Dogak Cinfwat.
 */

public class KeyStoreHelper {
    /**
     * The {@link KeyStore} instance.
     */
    KeyStore mStore;

    private RSAPublicKey mPublicKey;
    private RSAPrivateKey mPrivateKey;

    /**
     * Flag used to enable compatibility mode as not all Algorithms are supported in all API levels.
     */
    private boolean isCompatMode = false;

    public void loadKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        mStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        mStore.load(null);
    }

    public void generateRSAKeys(Context context) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, KeyStoreException {
        if (!mStore.containsAlias(RSA_KEY_ALIAS)) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 25);

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);

            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                    .setAlias(RSA_KEY_ALIAS)
                    .setKeySize(RSA_BIT_LENGTH)
                    .setKeyType(KeyProperties.KEY_ALGORITHM_RSA)
                    .setEndDate(end.getTime())
                    .setStartDate(start.getTime())
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(new X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                    .build();

            keyGen.initialize(spec);
            keyGen.generateKeyPair();
        }
    }

    public void loadRSAKeys() throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
        if (mStore.containsAlias(RSA_KEY_ALIAS) && mStore.entryInstanceOf(RSA_KEY_ALIAS, KeyStore.PrivateKeyEntry.class)) {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) mStore.getEntry(RSA_KEY_ALIAS, null);
            mPublicKey = (RSAPublicKey) entry.getCertificate().getPublicKey();
            mPrivateKey = (RSAPrivateKey) entry.getPrivateKey();
        }
    }


    /**
     * Generates AES key.
     *
     * @return the generated AES key.
     * @throws NoSuchAlgorithmException no such algorithm found error.
     */
    public byte[] generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");

        keyGen.init(AES_BIT_LENGTH);
        SecretKey sKey = keyGen.generateKey();
        return sKey.getEncoded();
    }
}
