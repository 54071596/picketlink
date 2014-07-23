package org.picketlink.json.jose.crypto;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static org.picketlink.json.JsonMessages.MESSAGES;

/**
 * The Class RSASignatureProvider.
 *
 * @author Pedro Igor
 */
public class RSASignatureProvider implements SignatureProvider {

    /** The Constant ALGORITHM. */
    private static final String ALGORITHM = "RSA";

    /** The instance. */
    private static RSASignatureProvider instance;

    /**
     * Instance.
     *
     * @return the signature provider
     */
    static final SignatureProvider instance() {
        if (instance == null) {
            instance = new RSASignatureProvider();
        }

        return instance;
    }

    /**
     * @see org.picketlink.json.jose.crypto.SignatureProvider#sign(byte[], org.picketlink.json.jose.crypto.Algorithm, byte[])
     */
    public byte[] sign(byte[] data, Algorithm algorithm, byte[] key) {
        try {
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            Signature signature = Signature.getInstance(algorithm.getAlgorithm());

            signature.initSign(privateKey);
            signature.update(data);

            return signature.sign();
        } catch (Exception e) {
            throw MESSAGES.cryptoSignatureFailed(algorithm, e);
        }
    }

    /**
     * @see org.picketlink.json.jose.crypto.SignatureProvider#verify(byte[], org.picketlink.json.jose.crypto.Algorithm, byte[],
     *      byte[])
     */
    public boolean verify(byte[] data, Algorithm algorithm, byte[] signature, byte[] key) {
        try {
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            Signature verifier = Signature.getInstance(algorithm.getAlgorithm());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            verifier.initVerify(publicKey);
            verifier.update(data);

            return verifier.verify(signature);
        } catch (Exception e) {
            throw MESSAGES.cryptoSignatureValidationFailed(algorithm, e);
        }

    }

}