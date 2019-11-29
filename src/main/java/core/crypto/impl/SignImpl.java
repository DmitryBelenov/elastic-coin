package core.crypto.impl;

import core.crypto.interfaces.Sign;

import java.io.*;
import java.security.*;

public class SignImpl implements Sign {

    private KeyPair keyPair;

    public void initKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");

        keyPairGenerator.initialize(1024, secureRandom);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    public SignedObject setSignature(String data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException {
        Signature signature = Signature.getInstance(privateKey.getAlgorithm());
        return new SignedObject(data, privateKey, signature);
    }

    public byte[] setByteSignature(String data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(privateKey.getAlgorithm());
        signature.initSign(privateKey);

        signature.update(data.getBytes());
        return signature.sign();
    }

    public boolean verifySignature(String data, byte[] signatureBytes, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(publicKey.getAlgorithm());
        signature.initVerify(publicKey);
        signature.update(data.getBytes());

        return signature.verify(signatureBytes);
    }

    public boolean verifySignature(SignedObject signedData, PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance(publicKey.getAlgorithm());
        return signedData.verify(publicKey, signature);
    }

    public String getUnsignedData(SignedObject signedData) throws IOException, ClassNotFoundException {
        return (String) signedData.getObject();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public void saveKey(String filePath, Key key) throws IOException, KeyManagementException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        file.createNewFile();

            if (key != null) {
                FileOutputStream fos = new FileOutputStream(filePath);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(key);

                oos.close();
                fos.close();
            } else {
                throw new KeyManagementException("No key for saving");
            }
    }

    @SuppressWarnings("unchecked")
    public <K> K readKey(String filePath) throws KeyManagementException {
        Key key;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis);

            key = (Key) ois.readObject();
        } catch (Exception e) {
            throw new KeyManagementException("Unable to read key, cause:\n" + e);
        }
        return (K) key;
    }
}
