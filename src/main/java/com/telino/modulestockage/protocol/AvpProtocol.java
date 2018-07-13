package com.telino.modulestockage.protocol;

/**
 * Les constants du protocole de communication entre ArchivageServeur et Module
 * Stockage Ils sont regroupés par certaines classes internes
 * 
 * @author Jiliang.WANG
 *
 */
public final class AvpProtocol {
	private AvpProtocol() {
		throw new AssertionError("Instantiation not allowed!");
	}

	/**
	 * Constant de commande
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum Commande {
		GET_FILE("get"), 
		CHECK_FILES("checkfiles");

		private String val;

		private Commande(String val) {
			this.val = val;
		}

		public String toString() {
			return this.val;
		}
	}
	
	/**
	 * Constant de commande
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum BackgroundService {
		CREATELOGARCHIVE(" "), 
		CREATELOGEVENT(" "),
		DESTROY(" "),
		CHECKFILES("Contrôle de l'intégralité des archives par le module de stockage ");
		
		private String detail;

		private BackgroundService(String detail) {
			this.detail = detail;
		}

		public String getDetail() {
			return this.detail;
		}
	}

	/**
	 * Constant de code de retour
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum ReturnCode {
		OK, KO, ERROR;
	}

	/**
	 * Constant de code error de contrôler de fichier
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum FileReturnError {
		HASH_NOT_MATCH_ERROR("Le hash de log_archive n’égale pas l’empreinte de l’archivage de DB "), 
		NOT_FOUND_ERROR("L’archive n’existe pas dans stockage "), 
		DECRYPT_ERROR("Le décryptage de l’archive est échoué "), 
		SHA_HASH_ERROR("Le hachage de calcul d’empreinte est échoué "),
		ENTIRETY_ERROR("Le contrôle de l’intégralité de l’archive est échoué ");

		private String detail;

		private FileReturnError(String detail) {
			this.detail = detail;
		}

		public String getDetail() {
			return this.detail;
		}
	}
}
