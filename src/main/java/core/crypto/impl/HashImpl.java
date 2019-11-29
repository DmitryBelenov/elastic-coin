package core.crypto.impl;

import com.google.common.hash.Hashing;
import core.crypto.interfaces.Hash;

public class HashImpl implements Hash {

    public String getHash(byte[] data) {
        return Hashing.sha256().hashBytes(data).toString().toUpperCase();
    }
}
