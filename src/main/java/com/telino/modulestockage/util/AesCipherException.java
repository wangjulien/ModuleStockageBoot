package com.telino.modulestockage.util;

/**
 * Exception d'emcapsulation de toutes Exception du AES Cipher (javax.crypto.Cipher)
 * 
 * @author Jiliang.WANG
 *
 */
public class AesCipherException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7450425461813521642L;

	public AesCipherException() {
		super();
	}

	public AesCipherException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AesCipherException(String message, Throwable cause) {
		super(message, cause);
	}

	public AesCipherException(String message) {
		super(message);
	}

	public AesCipherException(Throwable cause) {
		super(cause);
	}	
}
