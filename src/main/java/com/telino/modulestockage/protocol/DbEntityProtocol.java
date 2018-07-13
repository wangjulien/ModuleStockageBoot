package com.telino.modulestockage.protocol;

/**
 * Les constants utilise dans les DB entities 
 * 
 * @author Jiliang.WANG
 *
 */
public final class DbEntityProtocol {
	private DbEntityProtocol() {
		throw new AssertionError("Instantiation not allowed!");
	}

	/**
	 * Type code dans log_archive
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum LogArchiveType {
		A("Actions sur les archives "), 
		C("Contrôle d'intégrité de l'archive "), 
		S("Scellement des journaux "), 
		P("Action sur les profils ");

		private String detail;

		private LogArchiveType(String detail) {
			this.detail = detail;
		}

		public String getDetail() {
			return this.detail;
		}
	}
	
	/**
	 * Type code dans log_event
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum LogEventType {
		C("Contrôle d'intégrité de l'archive "), 
		S("Scellement "), 
		E("Aplliation ");

		private String detail;

		private LogEventType(String detail) {
			this.detail = detail;
		}

		public String getDetail() {
			return this.detail;
		}
	}
	
	
	/**
	 * Type status dans document
	 * 
	 * @author Jiliang.WANG
	 *
	 */
	public static enum DocumentStatut {
		REARDY_FOR_ARCHIVE("0"), 
		ARCHIVED("1"), 
		ATTESTATION("2");

		private String detail;

		private DocumentStatut(String detail) {
			this.detail = detail;
		}

		public String getDetail() {
			return this.detail;
		}
	}
	
}
