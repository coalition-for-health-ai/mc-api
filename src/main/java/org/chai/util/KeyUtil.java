package org.chai.util;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.chai.StampFunction;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

public class KeyUtil {
    public static PublicKey getPublicKey() {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
                    KeyUtil.class.getResourceAsStream("/public_key.der").readAllBytes()));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey getPrivateKey() {
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(new SecretClientBuilder()
                            .vaultUrl("https://" + System.getenv("KEY_VAULT_NAME") + ".vault.azure.net")
                            .credential(new DefaultAzureCredentialBuilder().build())
                            .buildClient().getSecret("secret-mc-api-private").getValue())));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
