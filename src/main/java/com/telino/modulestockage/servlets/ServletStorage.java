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
import java.util.Objects;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telino.avp.dto.DocumentDto;
import com.telino.avp.protocol.AvpProtocol.Commande;
import com.telino.avp.protocol.AvpProtocol.FileReturnError;
import com.telino.avp.protocol.AvpProtocol.ReturnCode;
import com.telino.modulestockage.service.StorageAction;

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

	public static final String SEPTOR = "/";

	// Le chemin d'acces pour <storage> est chargé depuis un fichier de
	// configuration.
	// S'il n'est pas trouvé, une valeur "home/tomcate/storage" par défaut est
	// utilisée.
	@Value("${app.storagePath:home/tomcate/storage}")
	private String RACINE;

	@Autowired
	private StorageAction storageAction;

	@PostMapping("/StorageService")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try {
			// Return information
			JSONObject result = new JSONObject();
			ReturnCode codeRetour = ReturnCode.KO;
			String message = "";

			// Input information
			Object inputStream = lecture(req);
			JSONObject trame = new JSONObject(inputStream.toString());
			String empreinte = null;

			LOGGER.debug(trame.toString());
			LOGGER.debug(RACINE);

			// Exploit the command
			String commandeStr = (String) trame.get("command");
			if (Objects.isNull(commandeStr)) {
				message = "action demandée inconnue";
			} else {

				Commande commande = Commande.getEnum(commandeStr);

				switch (commande) {

				// initialise une unité de stockage
				case INIT_STORAGE_UNIT:
					LOGGER.info("demande d'initialisation d'une unité de stockage (command=initstorageunit)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					try {
						File dir = new File(RACINE + SEPTOR + trame.get("idstorage"));
						dir.mkdirs();
						codeRetour = ReturnCode.OK;
					} catch (SecurityException se) {
						LOGGER.error("ServletStorage - Erreur creation unitstorage : {}", se.getMessage());
						message = se.getMessage();
					}

					break;

				case CREATE_STORAGE_UNIT:
					LOGGER.info("demande de création d'une unité de stockage (command=createstorageunit)");

					SecureRandom random = new SecureRandom();
					try {
						String idStorage = new BigInteger(130, random).toString(32);
						while ((new File(idStorage)).isDirectory()) {
							idStorage = new BigInteger(130, random).toString(32);
						}
						LOGGER.debug("ServletStorage - idStorage : {} ", idStorage);

						File directory = new File(RACINE + SEPTOR + idStorage);

						directory.mkdirs();
						codeRetour = ReturnCode.OK;
						message = idStorage;
					} catch (SecurityException se) {
						LOGGER.error("ServletStorage - Erreur creation unitstorage : {} ", se.getMessage());
						message = se.getMessage();
					}

					break;

				// archivage d'un fichier
				case ARCHIVE:
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

					// storageAction = new StorageAction());
					try {
						String contentBase64 = (String) trame.get("content");
						byte[] content = Base64.getDecoder().decode(contentBase64);
						empreinte = (String) trame.get("empreinte").toString();
						String idStorage = RACINE + SEPTOR + (String) trame.get("idstorage");

						storageAction.writeFile(idStorage, empreinte, content);
						codeRetour = ReturnCode.OK;
					} catch (Exception e) {
						LOGGER.error("ServletStorage - Erreur d'écriture du fichier (" + empreinte + ") : "
								+ e.getMessage());
						message = e.getMessage();
					}

					break;

				// recupération du contenu d'un fichier
				case GET_DOC:
					LOGGER.info("demande de recupération d'archive (command=get)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					if (trame.get("empreinte").toString() == null || trame.get("empreinte").toString() == "") {
						message = "Empreinte unique servant au stockage non communiquée";
						break;
					}

					try {
						empreinte = (String) trame.get("empreinte").toString();
						String idStorage = RACINE + SEPTOR + (String) trame.get("idstorage");

						byte[] content = storageAction.getFile(idStorage, empreinte);

						String base64content = Base64.getEncoder().encodeToString(content);
						result.put("content", base64content);
						codeRetour = ReturnCode.OK;
					} catch (Exception e) {
						message = e.toString();
					}

					break;

				// suppression d'un fichier
				case DELETE:
					LOGGER.info("demande de suppression d'archive (command=delete)");

					if (trame.get("idstorage") == null || trame.get("idstorage") == "") {
						message = "id du module de stockage non communiqué";
						break;
					}

					if (trame.get("empreinte").toString() == null || trame.get("empreinte").toString() == "") {
						message = "Empreinte unique servant au stockage non communiquée";
						break;
					}

					// storageAction = new StorageAction(RACINE + SEPTOR + (String)
					// trame.get("idstorage"));

					try {
						empreinte = (String) trame.get("empreinte").toString();
						String idStorage = RACINE + SEPTOR + (String) trame.get("idstorage");

						if (storageAction.deleteFile(idStorage, empreinte)) {
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
				case CHECK_FILES:
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
					List<DocumentDto> documents = Arrays
							.asList(jsonMapper.readValue(trame.get("documents").toString(), DocumentDto[].class));

					try {
						// Eventuellement une liste de documents en problème et la raison
						Map<UUID, FileReturnError> badDocs = new HashMap<>();
						String idStorage = RACINE + SEPTOR + (String) trame.get("idstorage");

						// contrôle l'intégralité des documents
						if (storageAction.checkFiles(idStorage, documents, badDocs)) {
							codeRetour = ReturnCode.OK;

							LOGGER.info("contrôle d'intégralité d'une liste d'archive reussi");
						} else {
							// la liste de docId dont le contrôle n'est pas validée
							message = jsonMapper.writeValueAsString(badDocs);
							codeRetour = ReturnCode.ERROR;
						}

					} catch (Exception e) {
						message = e.getMessage();
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

		} catch (Exception e) {
			LOGGER.error(e.getMessage());

			ecriture(resp, e.getMessage());

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
