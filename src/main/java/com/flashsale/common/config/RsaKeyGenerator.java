
package com.flashsale.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class RsaKeyGenerator {

    public static void main(String[] args) throws Exception {

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        String privateKey = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded());

        String publicKey = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPublic().getEncoded());

        String privatePem =
                "-----BEGIN PRIVATE KEY-----\n"
                        + privateKey
                        + "\n-----END PRIVATE KEY-----";

        String publicPem =
                "-----BEGIN PUBLIC KEY-----\n"
                        + publicKey
                        + "\n-----END PUBLIC KEY-----";

        Path directory = Path.of("src/main/resources/keys");

        Files.createDirectories(directory);

        Files.writeString(directory.resolve("private.pem"), privatePem);
        Files.writeString(directory.resolve("public.pem"), publicPem);

        System.out.println("RSA keys generated successfully!");
    }
}
