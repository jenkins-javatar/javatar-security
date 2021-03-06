/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pro.javatar.security.jwt.bean.jws.crypto;


import pro.javatar.security.jwt.bean.jws.Algorithm;
import pro.javatar.security.jwt.bean.jws.JWSInput;
import pro.javatar.security.jwt.exception.JwtException;
import pro.javatar.security.jwt.utils.Base64Url;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HMACProvider implements SignatureProvider {
    private static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case HS256:
                return "HMACSHA256";
            case HS384:
                return "HMACSHA384";
            case HS512:
                return "HMACSHA512";
            default:
                throw new IllegalArgumentException("Not a MAC Algorithm");
        }
    }

    private static Mac getMAC(final Algorithm alg) {

        try {
            return Mac.getInstance(getJavaAlgorithm(alg));

        } catch (NoSuchAlgorithmException e) {
            throw new JwtException("Unsupported HMAC algorithm: " + e.getMessage(), e);
        }
    }

    public static byte[] sign(byte[] data, Algorithm algorithm, byte[] sharedSecret) {
        try {
            Mac mac = getMAC(algorithm);
            mac.init(new SecretKeySpec(sharedSecret, mac.getAlgorithm()));
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new JwtException(e.getMessage(), e);
        }
    }

    public static byte[] sign(byte[] data, Algorithm algorithm, SecretKey key) {
        try {
            Mac mac = getMAC(algorithm);
            mac.init(key);
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new JwtException(e.getMessage(), e);
        }
    }

    public static boolean verify(JWSInput input, SecretKey key) {
        try {
            byte[] signature = sign(input.getEncodedSignatureInput().getBytes("UTF-8"), input.getHeader().getAlgorithm(), key);
            return MessageDigest.isEqual(signature, Base64Url.decode(input.getEncodedSignature()));
        } catch (Exception e) {
            throw new JwtException(e.getMessage(), e);
        }
    }


    public static boolean verify(JWSInput input, byte[] sharedSecret) {
        try {
            byte[] signature = sign(input.getEncodedSignatureInput().getBytes("UTF-8"), input.getHeader().getAlgorithm(), sharedSecret);
            return MessageDigest.isEqual(signature, Base64Url.decode(input.getEncodedSignature()));
        } catch (Exception e) {
            throw new JwtException(e.getMessage(), e);
        }
    }

    @Override
    public boolean verify(JWSInput input, String key) {
        return false;
    }
}
