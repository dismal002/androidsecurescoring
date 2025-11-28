package com.security.scoringengine.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecureConfigStorage {
    private static final String KEYSTORE_ALIAS = "ScoringEngineKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CONFIG_FILE = "scoring_config.enc";

    private Context context;

    public SecureConfigStorage(Context context) {
        this.context = context;
    }

    public void saveConfig(String jsonConfig) throws Exception {
        SecretKey key = getOrCreateKey();
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(jsonConfig.getBytes("UTF-8"));
        
        // Store IV + encrypted data
        File file = new File(context.getFilesDir(), CONFIG_FILE);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(iv.length);
        fos.write(iv);
        fos.write(encrypted);
        fos.close();
        
        // Set file permissions to be readable only by this app
        file.setReadable(false, false);
        file.setReadable(true, true);
        file.setWritable(false, false);
        file.setWritable(true, true);
    }

    public String loadConfig() throws Exception {
        File file = new File(context.getFilesDir(), CONFIG_FILE);
        if (!file.exists()) {
            return null;
        }
        
        FileInputStream fis = new FileInputStream(file);
        int ivLength = fis.read();
        byte[] iv = new byte[ivLength];
        fis.read(iv);
        
        byte[] encrypted = new byte[(int) file.length() - ivLength - 1];
        fis.read(encrypted);
        fis.close();
        
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, "UTF-8");
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
            
            keyGenerator.init(keyGenParameterSpec);
            return keyGenerator.generateKey();
        }
        
        return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
    }
}
