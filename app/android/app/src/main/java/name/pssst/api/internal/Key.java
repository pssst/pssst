/*
 * Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package name.pssst.api.internal;

import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.openssl.PEMDecryptorProvider;
import org.spongycastle.openssl.PEMEncryptedKeyPair;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.spongycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import name.pssst.api.PssstException;
import name.pssst.api.internal.entity.AesData;
import name.pssst.api.internal.entity.RsaData;

/**
 * Internal key class providing cryptographic methods.
 */
public final class Key {
    private static final int RSA_KEY_SIZE = 4096;
    private static final int AES_KEY_SIZE = 32;
    private static final int AES_IV_SIZE = 16;
    private static final int NONCE_SIZE = AES_KEY_SIZE + AES_IV_SIZE;

    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES";
    private static final String SHA_ALGORITHM = "SHA1";
    private static final String RSA_SIGNATURE = "SHA512withRSA";
    private static final String RSA_CIPHER = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    private static final String AES_CIPHER = "AES/CFB8/NOPADDING";
    private static final String MAC_CIPHER = "HmacSHA512";
    private static final String PEM_CIPHER = "DES-EDE3-CBC";

    private final KeyPair mKey;

    /**
     * Initializes the key.
     * @param key Key
     */
    private Key(KeyPair key) {
        mKey = key;
    }

    /**
     * Returns the loaded key.
     * @param data Key data
     * @param password Key password
     * @return Key
     * @throws PssstException
     */
    public static Key parse(String data, String password) throws PssstException {
        return new Key(decodeKey(data, password));
    }

    /**
     * Returns the loaded key.
     * @param data Key data
     * @return Key
     * @throws PssstException
     */
    public static Key parse(String data) throws PssstException {
        return new Key(decodeKey(data, null));
    }

    /**
     * Returns a new generated key.
     * @return Key
     * @throws PssstException
     */
    public static Key generate() throws PssstException {
        final KeyPairGenerator generator;

        try {
            generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(RSA_KEY_SIZE, new SecureRandom());

            return new Key(generator.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Generator not found", e);
        }
    }

    /**
     * Returns the key fingerprint.
     * @param data Key data
     * @return Fingerprint
     * @throws PssstException
     */
    public static String fingerprint(String data) throws PssstException {
        try {
            return Hex.toHexString(MessageDigest.getInstance(SHA_ALGORITHM).digest(data.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Algorithm not found", e);
        }
    }

    /**
     * Returns the private part encrypted with the password.
     * @param password Key password
     * @return Private part
     * @throws PssstException
     */
    public final String exportPrivate(String password) throws PssstException {
        assertPrivateKey();

        return encodeKey(password);
    }

    /**
     * Returns the public part.
     * @return Public part
     * @throws PssstException
     */
    public final String exportPublic() throws PssstException {
        assertPublicKey();

        return encodeKey(null);
    }

    /**
     * Returns the encrypted data.
     * @param data Decrypted data
     * @return Encrypted data
     * @throws PssstException
     */
    public final AesData encrypt(byte[] data) throws PssstException {
        assertPublicKey();

        final byte[] nonce = generateNonce();

        return new AesData(encryptData(data, nonce), encryptNonce(nonce));
    }

    /**
     * Returns the decrypted data.
     * @param aesData Encrypted data
     * @return Decrypted data
     * @throws PssstException
     */
    public final byte[] decrypt(AesData aesData) throws PssstException {
        assertPrivateKey();

        final byte[] nonce = decryptNonce(aesData.getNonce());

        return decryptData(aesData.getData(), nonce);
    }

    /**
     * Returns the data signature.
     * @param data Decrypted data
     * @return Signature
     * @throws PssstException
     */
    public final RsaData sign(byte[] data) throws PssstException {
        assertPrivateKey();

        final Signature signature;
        final long timestamp = generateTimestamp();

        try {
            signature = Signature.getInstance(RSA_SIGNATURE);
            signature.initSign(mKey.getPrivate());
            signature.update(hashData(data, timestamp));

            return new RsaData(signature.sign(), timestamp);
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Signature not found", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        } catch (SignatureException e) {
            throw new PssstException("Signature error", e);
        }
    }

    /**
     * Returns if the data could be verified.
     * @param data Encrypted data
     * @param rsaData Signature
     * @return Verification
     * @throws PssstException
     */
    public final boolean verify(byte[] data, RsaData rsaData) throws PssstException {
        assertPublicKey();

        final Signature signature;

        try {
            signature = Signature.getInstance(RSA_SIGNATURE);
            signature.initVerify(mKey.getPublic());
            signature.update(hashData(data, rsaData.getTimestamp()));

            return signature.verify(rsaData.getSignature());
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Signature not found", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        } catch (SignatureException e) {
            throw new PssstException("Signature error", e);
        }
    }

    /**
     * Returns a new nonce.
     * @return Nonce
     */
    private static byte[] generateNonce() {
        final byte[] nonce = new byte[NONCE_SIZE];

        new SecureRandom().nextBytes(nonce);

        return nonce;
    }

    /**
     * Returns a new timestamp as UNIX epoch seconds.
     * @return Timestamp
     */
    private static long generateTimestamp() {
        return (System.currentTimeMillis() / 1000);
    }

    /**
     * Returns the nonces key part.
     * @param nonce Nonce
     * @return Key
     */
    private static SecretKeySpec createKey(byte[] nonce) {
        final byte[] key = new byte[AES_KEY_SIZE];

        System.arraycopy(nonce, 0, key, 0, AES_KEY_SIZE);

        return new SecretKeySpec(key, AES_ALGORITHM);
    }

    /**
     * Returns the nonces IV part.
     * @param nonce Nonce
     * @return IV
     */
    private static IvParameterSpec createIv(byte[] nonce) {
        final byte[] iv = new byte[AES_IV_SIZE];

        System.arraycopy(nonce, AES_KEY_SIZE, iv, 0, AES_IV_SIZE);

        return new IvParameterSpec(iv);
    }

    /**
     * Returns the timestamps time.
     * @param timestamp Timestamp
     * @return Time
     */
    private static SecretKeySpec createTime(long timestamp) {
        return new SecretKeySpec(String.valueOf(timestamp).getBytes(), MAC_CIPHER);
    }

    /**
     * Assert key has a private part.
     * @throws PssstException
     */
    private void assertPrivateKey() throws PssstException {
        if (mKey.getPrivate() == null) {
            throw new PssstException("Not a private key");
        }
    }

    /**
     * Assert key has a public part.
     * @throws PssstException
     */
    private void assertPublicKey() throws PssstException {
        if (mKey.getPublic() == null) {
            throw new PssstException("Not a public key");
        }
    }

    /**
     * Returns the encrypted data.
     * @param data Decrypted data
     * @param nonce Nonce
     * @return Encrypted data
     * @throws PssstException
     */
    private byte[] encryptData(byte[] data, byte[] nonce) throws PssstException {
        final Cipher cipher;

        try {
            cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, createKey(nonce), createIv(nonce));

            return cipher.doFinal(data, 0, data.length);
        } catch (NoSuchPaddingException e) {
            throw new PssstException("Padding not found", e);
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Cipher not found", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new PssstException("Cipher param invalid", e);
        } catch (IllegalBlockSizeException e) {
            throw new PssstException("Block size invalid", e);
        } catch (BadPaddingException e) {
            throw new PssstException("Padding invalid", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Returns the decrypted data.
     * @param data Encrypted data
     * @param nonce Nonce
     * @return Decrypted data
     * @throws PssstException
     */
    private byte[] decryptData(byte[] data, byte[] nonce) throws PssstException {
        final Cipher cipher;

        try {
            cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, createKey(nonce), createIv(nonce));

            return cipher.doFinal(data, 0, data.length);
        } catch (NoSuchPaddingException e) {
            throw new PssstException("Padding not found", e);
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Cipher not found", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new PssstException("Cipher param invalid", e);
        } catch (IllegalBlockSizeException e) {
            throw new PssstException("Block size invalid", e);
        } catch (BadPaddingException e) {
            throw new PssstException("Padding invalid", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Returns the encrypted nonce.
     * @param nonce Nonce
     * @return Encrypted nonce
     * @throws PssstException
     */
    private byte[] encryptNonce(byte[] nonce) throws PssstException {
        final Cipher cipher;

        try {
            cipher = Cipher.getInstance(RSA_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, mKey.getPublic());

            return cipher.doFinal(nonce, 0, nonce.length);
        } catch (NoSuchPaddingException e) {
            throw new PssstException("Padding not found", e);
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Cipher not found", e);
        } catch (IllegalBlockSizeException e) {
            throw new PssstException("Block size invalid", e);
        } catch (BadPaddingException e) {
            throw new PssstException("Padding invalid", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Returns the decrypted nonce.
     * @param nonce Nonce
     * @return Decrypted nonce
     * @throws PssstException
     */
    private byte[] decryptNonce(byte[] nonce) throws PssstException {
        final Cipher cipher;

        try {
            cipher = Cipher.getInstance(RSA_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, mKey.getPrivate());

            return cipher.doFinal(nonce, 0, nonce.length);
        } catch (NoSuchPaddingException e) {
            throw new PssstException("Padding not found", e);
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Cipher not found", e);
        } catch (IllegalBlockSizeException e) {
            throw new PssstException("Block size invalid", e);
        } catch (BadPaddingException e) {
            throw new PssstException("Padding invalid", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Returns the message hash.
     * @param data Message data
     * @param timestamp Timestamp
     * @return Hash
     * @throws PssstException
     */
    private byte[] hashData(byte[] data, long timestamp) throws PssstException {
        final Mac mac;

        try {
            mac = Mac.getInstance(MAC_CIPHER);
            mac.init(createTime(timestamp));

            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new PssstException("Mac not found", e);
        } catch (InvalidKeyException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Decodes the public or private key part.
     * @param data Key data
     * @param password Private key password
     * @return Key pair
     * @throws PssstException
     */
    private static KeyPair decodeKey(String data, String password) throws PssstException {
        final JcaPEMKeyConverter pemConverter = new JcaPEMKeyConverter();
        final PEMDecryptorProvider pemDecryptor;
        final PEMParser pemParser;
        final Object keyObject;

        try {
            pemParser = new PEMParser(new StringReader(data));
            keyObject = pemParser.readObject();
            pemParser.close();

            if (keyObject instanceof PEMEncryptedKeyPair) {
                pemDecryptor = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                return pemConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(pemDecryptor));
            } else if (keyObject instanceof PEMKeyPair) {
                return pemConverter.getKeyPair((PEMKeyPair) keyObject);
            } else if (keyObject instanceof SubjectPublicKeyInfo) {
                return new KeyPair(pemConverter.getPublicKey((SubjectPublicKeyInfo) keyObject), null);
            } else {
                throw new PssstException("Key invalid");
            }
        } catch (PEMException e) {
            throw new PssstException("Password wrong", e);
        } catch (IOException e) {
            throw new PssstException("Decode failed", e);
        }
    }

    /**
     * Encodes the public or private key part.
     * @param password Private key password
     * @return Key data
     * @throws PssstException
     */
    private String encodeKey(String password) throws PssstException {
        final StringWriter stringWriter = new StringWriter();
        final JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        final JcePEMEncryptorBuilder pemEncryptor;

        try {
            if (password != null && !password.isEmpty()) {
                pemEncryptor = new JcePEMEncryptorBuilder(PEM_CIPHER);
                pemWriter.writeObject(mKey.getPrivate(), pemEncryptor.build(password.toCharArray()));
            } else {
                pemWriter.writeObject(mKey.getPublic());
            }

            pemWriter.close();

            return stringWriter.toString();
        } catch (IOException e) {
            throw new PssstException("Encode failed", e);
        }
    }
}
