package com.telino.modulestockage.util;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Classe utilitaire pour Crypter/Décrypter d'un document avec Algo AES.
 * Classe static non instanciable
 * 
 * @author Jiliang.WANG
 *
 */
public final class AesCipher {

	// Paramètre de configuration pour AES Cipher
	private static final String ALGO_NAME = "AES";
	private static final String CIPHER_INSTANCE = "AES/CTR/NoPadding";
	
	private AesCipher() {
		throw new AssertionError("Instantiation not allowed!");
	}


	/**
	 * @param secretKey	: ByteArray d'un clé symétrique pour crypter avec AES
	 * @param bytesToEncrypt : contenu binary à crypter
	 * @return
	 * @throws AesCipherException
	 */
	public static Map<String, byte[]> encrypt(final byte[] secretKey, final byte[] bytesToEncrypt) throws AesCipherException {

		Map<String, byte[]> result = new HashMap<>();
		byte[] initVector = new byte[16];

		// create a new random initialization vector for AES
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(initVector);

		byte[] encryptedbytes = transform(Cipher.ENCRYPT_MODE, secretKey, initVector, bytesToEncrypt);
		result.put("iv", initVector);
		result.put("crypted", encryptedbytes);

		return result;
	}

	/**
	 * @param secretKey	: ByteArray d'un clé symétrique (même clé lors de cryptage) pour décrypter avec AES
	 * @param initVector : Un vecteur binaire généré aléatoirement lors de cryptage du document
	 * @param bytesToDecrypt : contenu binary à décrypter
	 * @return
	 * @throws AesCipherException
	 */
	public static byte[] decrypt(final byte[] secretKey, final byte[] initVector, final byte[] bytesToDecrypt)
			throws AesCipherException {
		return transform(Cipher.DECRYPT_MODE, secretKey, initVector, bytesToDecrypt);
	}


	/**
	 * @param mode : Cryptage ou Décryptage
	 * @param keyBytes
	 * @param ivBytes
	 * @param messageBytes
	 * @return
	 * @throws AesCipherException
	 */
	private static byte[] transform(final int mode, final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes)
			throws AesCipherException {
		final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGO_NAME);
		final IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		byte[] transformedBytes = null;

		try {
			final Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);

			cipher.init(mode, keySpec, ivSpec);

			transformedBytes = cipher.doFinal(messageBytes);
		} catch (Exception e) {
			throw new AesCipherException(e);
		}
		return transformedBytes;
	}
}