package com.telino.modulestockage.servlets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telino.modulestockage.dto.DocumentBean;
import com.telino.modulestockage.protocol.AvpProtocol.FileReturnError;
import com.telino.modulestockage.protocol.AvpProtocol.ReturnCode;
import com.telino.modulestockage.service.StorageAction;
import com.telino.modulestockage.util.ConfigFile;

/**
 * Servlet servi comme point d'entrée et dispatcher pour toutes commandes
 * envoyées par Archivage Serveur
 * 
 * @author
 *
 */

@Controller
@RequestMapping("/modulestockage")
public class ServletStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServletStorage.class);

	private SecureRandom random = new SecureRandom();

	// Le chemin d'acces pour <storage> est chargé depuis un fichier de
	// configuration.
	// S'il n'est pas trouvé, une valeur "home/tomcate/storage" par défaut est
	// utilisée.
	private static final String RACINE = (null != ConfigFile.PROPERTIES.get("StoragePath")
			? ConfigFile.PROPERTIES.get("StoragePath")
			: "home/tomcate/storage");
	private static final String SEPTOR = "/";

	@PostMapping("/StorageService")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try {
			JSONObject result = new JSONObject();
			Object message = "";
			ReturnCode codeRetour = ReturnCode.KO;
			StorageAction storageAction;
			String empreinte;
			byte[] content = new byte[0];

			JSONObject trame = null;
			Object inputStream = lecture(req);
			trame = new JSONObject(inputStream.toString());

			LOGGER.debug(trame.toString());

			LOGGER.debug(RACINE);

			String commande = trame.get("command").toString();

			if (commande == null) {
				message = "action demandée inconnue";
			} else {

				switch (commande) {

				// initialise une unité de stockage
				case "initstorageunit":
					LOGGER.info("demande d'initialisation d'une unité de stockage (command=initstorageunit)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					File dir = new File(RACINE + SEPTOR + trame.get("idstorage"));
					try {
						dir.mkdirs();
						codeRetour = ReturnCode.OK;
					} catch (SecurityException se) {
						LOGGER.error("ServletStorage - Erreur creation unitstorage : {}", se.getMessage());
						message = se;
					}

					break;

				case "createstorageunit":
					LOGGER.info("demande de création d'une unité de stockage (command=createstorageunit)");

					String idStorage = new BigInteger(130, random).toString(32);
					while ((new File(idStorage)).isDirectory()) {
						idStorage = new BigInteger(130, random).toString(32);
					}
					LOGGER.debug("ServletStorage - idStorage : {} ", idStorage);

					File directory = new File(RACINE + SEPTOR + idStorage);

					try {
						directory.mkdirs();
						codeRetour = ReturnCode.OK;
						message = idStorage;
					} catch (SecurityException se) {
						LOGGER.error("ServletStorage - Erreur creation unitstorage : {} ", se.getMessage());
						message = se;
					}

					break;

				// archivage d'un fichier
				case "archive":
					LOGGER.info("demande d'archivage (command=archive)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					if (trame.get("empreinte").toString() == null || trame.get("empreinte").toString() == "") {
						message = "Empreinte unique servant au stockage non communiquée";
						break;
					}

					if (trame.get("content") == null || trame.get("content") == "") {
						message = "Contenu du fichier à archiver vide";
						break;
					}

					storageAction = new StorageAction(RACINE + SEPTOR + (String) trame.get("idstorage"));
					String contentBase64 = (String) trame.get("content");
					content = Base64.getDecoder().decode(contentBase64);
					empreinte = (String) trame.get("empreinte").toString();

					try {
						storageAction.writeFile(empreinte, content);
						codeRetour = ReturnCode.OK;
					} catch (Exception e) {
						LOGGER.error("ServletStorage - Erreur d'écriture du fichier (" + empreinte + ") : "
								+ e.getMessage());
						message = e;
					}

					break;

				// recupération du contenu d'un fichier
				case "get":
					LOGGER.info("demande de recupération d'archive (command=get)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					if (trame.get("empreinte").toString() == null || trame.get("empreinte").toString() == "") {
						message = "Empreinte unique servant au stockage non communiquée";
						break;
					}

					storageAction = new StorageAction(RACINE + SEPTOR + (String) trame.get("idstorage"));
					empreinte = (String) trame.get("empreinte").toString();

					try {
						content = storageAction.getFile(empreinte);
						String base64content = new String(Base64.getEncoder().encode(content), "UTF-8");
						result.put("content", base64content);
						codeRetour = ReturnCode.OK;
					} catch (Exception e) {
						message = e;
					}

					break;

				// suppression d'un fichier
				case "delete":
					LOGGER.info("demande de suppression d'archive (command=delete)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					if (trame.get("empreinte").toString() == null || trame.get("empreinte").toString() == "") {
						message = "Empreinte unique servant au stockage non communiquée";
						break;
					}

					storageAction = new StorageAction(RACINE + SEPTOR + (String) trame.get("idstorage"));
					empreinte = (String) trame.get("empreinte").toString();

					try {
						if (storageAction.deleteFile(empreinte)) {
							codeRetour = ReturnCode.OK;
						} else {
							message = "Impossible de supprimer le fichier";
						}
					} catch (IOException e) {
						message = "Impossible de supprimer le fichier : " + e.getMessage();
					}

					break;

				// case "deletestorageunit":
				// break;

				// Check list of documents
				case "checkfiles":
					LOGGER.info("demande de contrôle d'intégralité d'une liste d'archive (command=checkfiles)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					if (trame.get("documents").toString() == null || trame.get("documents").toString() == "") {
						message = "Liste Documents à controler non communiquée";
						break;
					}

					// Récupérer liste de document depuis json String
					ObjectMapper jsonMapper = new ObjectMapper();
					List<DocumentBean> documents = Arrays
							.asList(jsonMapper.readValue(trame.get("documents").toString(), DocumentBean[].class));

					storageAction = new StorageAction(RACINE + SEPTOR + (String) trame.get("idstorage"));
					try {
						// Eventuellement une liste de documents en problème et la raison
						Map<Long, FileReturnError> badDocs = new HashMap<>();

						// contrôle l'intégralité des documents
						if (storageAction.checkFiles(documents, badDocs)) {
							codeRetour = ReturnCode.OK;
							
							LOGGER.info("contrôle d'intégralité d'une liste d'archive reussi");
						} else {
							// la liste de docId dont le contrôle n'est pas validée
							message = jsonMapper.writeValueAsString(badDocs);
							codeRetour = ReturnCode.ERROR;
						}

					} catch (Exception e) {
						message = e;
					}

					break;

				default:
					message = "action demandée(" + commande + ") inconnue";
				}
			}
			LOGGER.debug("code retour : " + codeRetour.toString() + ", message : " + message);

			result.put("codeRetour", codeRetour.toString());
			result.put("message", message);

			
			ecriture(resp, result.toString());

		} catch (JSONException |

				IOException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();

			ecriture(resp, e.getMessage().toString());

		}
	}

	private static Object lecture(HttpServletRequest request) {
		Object trame = null;

		try (InputStream in = request.getInputStream();
				ObjectInputStream inputFromApplet = new ObjectInputStream(in);) {

			trame = inputFromApplet.readObject();

		} catch (Exception e) {
			trame = "";
			LOGGER.error(e.getStackTrace().toString());
			// e.printStackTrace();
		}
		return trame;
	}
	
	private static void ecriture(HttpServletResponse response, String result) {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		try (OutputStream outstr = response.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(outstr);) {

			oos.writeObject(result);
			oos.flush();

		} catch (Exception e) {
			LOGGER.error(e.getStackTrace().toString());
			// e.printStackTrace();
		}
	}
}
