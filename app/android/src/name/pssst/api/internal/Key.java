// Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
package name.pssst.api.internal;

import android.util.Pair;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.openssl.*;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.openssl.jcajce.JcaPKCS8Generator;
import org.spongycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.spongycastle.operator.InputDecryptorProvider;
import org.spongycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.spongycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;

/**
 * Class for providing cryptographic methods.
 * @author Christian & Christian
 */
public final class Key {
    private final static int SIZE = 4096;
    private final static long GRACE = 30;

    private KeyPair pair;

    /**
     * Initializes the internal key pair.
     * @param pair key pair
     */
    private Key(KeyPair pair) {
        this.pair = pair;
    }

    /**
     * Returns a new private key instance.
     * @param key private key in PEM format
     * @param password password
     * @return key
     * @throws Exception
     */
    public static Key fromPrivateKey(String key, String password) throws Exception {
        PEMParser pem = new PEMParser(new StringReader(key));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        InputDecryptorProvider pkcs8 = new JcePKCSPBEInputDecryptorProviderBuilder().build(password.toCharArray());

        Object obj = pem.readObject();
        PrivateKeyInfo info;

        if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
            info = ((PKCS8EncryptedPrivateKeyInfo) obj).decryptPrivateKeyInfo(pkcs8);
        } else {
            info = ((PEMKeyPair) obj).getPrivateKeyInfo();
        }
        pem.close();

        return new Key(buildKeyPair(converter.getPrivateKey(info)));
    }

    /**
     * Returns a new public key instance.
     * @param key public key in PEM format
     * @return key
     * @throws Exception
     */
    public static Key fromPublicKey(String key) throws Exception {
        PEMParser pem = new PEMParser(new StringReader(key));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

        return new Key(new KeyPair(converter.getPublicKey((SubjectPublicKeyInfo) pem.readObject()), null));
    }

    /**
     * Returns a newly generated key.
     * @return key
     * @throws Exception
     */
    public static Key generateKey() throws Exception {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(SIZE);

        return new Key(rsa.generateKeyPair());
    }

    /**
     * Returns the encrypted private key in PEM format.
     * @param password password
     * @return private key
     * @throws Exception
     */
    public final String exportPrivateKey(String password) throws Exception {
        StringWriter key = new StringWriter();
        PEMWriter pem = new PEMWriter(key);

        JceOpenSSLPKCS8EncryptorBuilder pkcs8 = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES);
        pkcs8.setPasssword(password.toCharArray());
        JcaPKCS8Generator generator = new JcaPKCS8Generator(pair.getPrivate(), pkcs8.build());

        pem.writeObject(generator.generate());
        pem.close();

        return key.toString().replaceAll("\\r\\n", "\n");
    }

    /**
     * Returns the public key in PEM format.
     * @return public key
     * @throws Exception
     */
    public final String exportPublicKey() throws Exception {
        StringWriter key = new StringWriter();
        PEMWriter pem = new PEMWriter(key);

        pem.writeObject(pair.getPublic());
        pem.close();

        return key.toString().replaceAll("\\r\\n", "\n");
    }

    /**
     * Returns the encrypted data and once.
     * @param data data
     * @return data and once
     * @throws Exception
     */
    public final Pair<byte[], byte[]> encrypt(byte[] data) throws Exception {
        byte[] once = generateOnce(48);

        data = encryptData(data, once);
        once = encryptOnce(once);

        return new Pair<byte[], byte[]>(data, once);
    }

    /**
     * Returns the decrypted data.
     * @param data data
     * @param once once
     * @return data
     * @throws Exception
     */
    public final byte[] decrypt(byte[] data, byte[] once) throws Exception {
        once = decryptOnce(once);
        data = decryptData(data, once);

        return data;
    }

    /**
     * Returns timestamp and data signature.
     * @param data data
     * @return timestamp and signature
     */
    public final Pair<Long, byte[]> sign(byte[] data) throws Exception {
        long timestamp = generateTimestamp();

        Signature rsa = Signature.getInstance("SHA512withRSA");
        rsa.initSign(pair.getPrivate());
        rsa.update(hashData(timestamp, data));

        return new Pair<Long, byte[]>(timestamp, rsa.sign());
    }

    /**
     * Returns if data could be verified.
     * @param data data
     * @param timestamp timestamp
     * @param signature signature
     * @return verified
     */
    public final boolean verify(byte[] data, long timestamp, byte[] signature) throws Exception {
        long now = generateTimestamp();

        if (Math.abs(timestamp - now) > GRACE) {
            return false;
        }

        Signature rsa = Signature.getInstance("SHA512withRSA");
        rsa.initVerify(pair.getPublic());
        rsa.update(hashData(timestamp, data));

        return rsa.verify(signature);
    }

    /**
     * Returns the private key as a key pair.
     * @param key private key
     * @return key pair
     * @throws Exception
     */
    private static KeyPair buildKeyPair(PrivateKey key) throws Exception {
        StringWriter buffer = new StringWriter();
        PEMWriter writer = new PEMWriter(buffer);
        writer.writeObject(key);
        writer.close();

        StringReader reader = new StringReader(buffer.toString());
        PEMParser parser = new PEMParser(reader);
        Object obj = parser.readObject();
        parser.close();

        // Found no better way to do this
        return new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) obj);
    }

    /**
     * Returns the actual timestamp.
     * @return timestamp
     */
    private long generateTimestamp() {
        return (System.currentTimeMillis() / 1000);
    }

    /**
     * Returns a new random once.
     * @param size once size
     * @return once
     */
    private byte[] generateOnce(int size) {
        byte[] once = new byte[size];

        new SecureRandom().nextBytes(once);

        return once;
    }

    /**
     * Return the key spec.
     * @param once once
     * @return key spec
     */
    private SecretKeySpec createKey(byte[] once) {
        byte[] key = new byte[32];

        System.arraycopy(once, 0, key, 0, 32);

        return new SecretKeySpec(key, "AES");
    }

    /**
     * Returns the iv spec.
     * @param once once
     * @return iv spec
     */
    private IvParameterSpec createIv(byte[] once) {
        byte[] iv = new byte[16];

        System.arraycopy(once, 32, iv, 0, 16);

        return new IvParameterSpec(iv);
    }

    /**
     * Returns the key parameter.
     * @param timestamp timestamp
     * @return key parameter
     * @throws Exception
     */
    private KeyParameter createParam(long timestamp) throws Exception {
        return new KeyParameter(String.valueOf(timestamp).getBytes("US-ASCII"));
    }

    /**
     * Returns data hash.
     * @param timestamp timestamp
     * @param data data
     * @return hash
     * @throws Exception
     */
    private byte[] hashData(long timestamp, byte[] data) throws Exception {
        HMac hmac = new HMac(new SHA512Digest());

        byte[] hash = new byte[hmac.getMacSize()];

        hmac.init(createParam(timestamp));
        hmac.update(data, 0, data.length);
        hmac.doFinal(hash, 0);

        return hash;
    }

    /**
     * Returns the OAEP encrypted once.
     * @param once once
     * @return encrypted once
     * @throws Exception
     */
    private byte[] encryptOnce(byte[] once) throws Exception {
        Cipher oaep = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");

        oaep.init(Cipher.ENCRYPT_MODE, pair.getPublic());
        once = oaep.doFinal(once, 0, once.length);

        return once;
    }

    /**
     * Returns the AES encrypted data.
     * @param data data
     * @param once once
     * @return encrypted data
     * @throws Exception
     */
    private byte[] encryptData(byte[] data, byte[] once) throws Exception {
        Cipher aes = Cipher.getInstance("AES/CFB8/NOPADDING");

        aes.init(Cipher.ENCRYPT_MODE, createKey(once), createIv(once));
        data = aes.doFinal(data, 0, data.length);

        return data;
    }

    /**
     * Returns the OAEP decrypted once.
     * @param once once
     * @return decrypted once
     * @throws Exception
     */
    private byte[] decryptOnce(byte[] once) throws Exception {
        Cipher oaep = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");

        oaep.init(Cipher.DECRYPT_MODE, pair.getPrivate());
        once = oaep.doFinal(once, 0, once.length);

        return once;
    }

    /**
     * Returns the AES decrypted data.
     * @param data data
     * @param once once
     * @return decrypted data
     * @throws Exception
     */
    private byte[] decryptData(byte[] data, byte[] once) throws Exception {
        Cipher aes = Cipher.getInstance("AES/CFB8/NOPADDING");

        aes.init(Cipher.DECRYPT_MODE, createKey(once), createIv(once));
        data = aes.doFinal(data, 0, data.length);

        return data;
    }
}
