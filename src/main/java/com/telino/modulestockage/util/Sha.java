package com.telino.modulestockage.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe utilitaire pour hachage (avec algo 'SHA-256') d'une chaine de characters, servi comme l'impreinte
 * Classe static non instanciable
 * 
 * @author Jiliang.WANG
 *
 */
public final class Sha {
	
	private static final String ALGO_HASH = "SHA-256";
	
	public static final String SEPARATOR = "+";
	
	private Sha() {
		throw new AssertionError("Instantiation not allowed!");
	}

	/**
	 * @param src : chaine de charater à hacher
	 * @param codage : type d'encodage (ex. UTF-8)
	 * @return : String de résultat d'hachage
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String encode(String src, String codage) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance(ALGO_HASH);
		byte bytes[] = src.getBytes(codage);
		md.update(bytes, 0, bytes.length);
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}

	private static String convertToHex(byte[] sha1hash) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < sha1hash.length; i++) {
			byte c = sha1hash[i];
			addHex(builder, (c >> 4) & 0xf);
			addHex(builder, c & 0xf);
		}
		return builder.toString();
	}

	private static void addHex(StringBuilder builder, int c) {
		if (c < 10)
			builder.append((char) (c + '0'));
		else
			builder.append((char) (c + 'a' - 10));
	}
}
