package com.telino.modulestockage.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telino.modulestockage.dto.DocumentBean;
import com.telino.modulestockage.protocol.AvpProtocol.FileReturnError;
import com.telino.modulestockage.util.AesCipher;
import com.telino.modulestockage.util.AesCipherException;
import com.telino.modulestockage.util.ConfigFile;
import com.telino.modulestockage.util.Sha;

/**
 * Classe d'actions - excuter toutes les commandes dispatchées par
 * StorageServlet
 * 
 * @author
 *
 */
public class StorageAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(StorageAction.class);

	private static final String SEPTOR = "/";
	private static final int MAX_CHECK_FILES_BY_THREAD = (null != ConfigFile.PROPERTIES.get("MaxCheckFilesByThread")
			? Integer.valueOf(ConfigFile.PROPERTIES.get("MaxCheckFilesByThread"))
			: 100); // Default files package per thread
	private static final long CHECK_FILES_TASK_TIMEOUT = (null != ConfigFile.PROPERTIES.get("CheckFilesTaskTimeout")
			? Long.valueOf(ConfigFile.PROPERTIES.get("CheckFilesTaskTimeout"))
			: 60L); // Default timeout value 60 minutes 
	private static final int MAX_CHECK_FILES_THREAD = (null != ConfigFile.PROPERTIES.get("MaxCheckFilesThread")
			? Integer.valueOf(ConfigFile.PROPERTIES.get("MaxCheckFilesThread"))
			: 5);
	
	
	private String idStorage;

	public StorageAction(String idStorage) {
		super();
		this.idStorage = idStorage;
	}

	/**
	 * Verifier l'existence d'une unité de stockage
	 * 
	 * @param idStrorage
	 * @return
	 */
	public boolean existStorage(String idStrorage) {
		String dirName = idStorage;
		if (!(new File(dirName)).isDirectory()) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Ecriture physique d'un fichier
	 * 
	 * @param sha1Unique
	 *            l'identifiant de stockage
	 * @param content
	 *            le contenu du fichier
	 * @throws Exception
	 */
	public void writeFile(String sha1Unique, byte[] content) throws Exception {
		Boolean dirExist = true;

		String dirName = idStorage + SEPTOR + sha1Unique.substring(0, 2);
		String fileName = sha1Unique.substring(2);

		if (!(new File(dirName)).isDirectory()) {
			dirExist = false;
			dirExist = (new File(dirName)).mkdirs();
		}
		if (dirExist) {
			FileUtils.writeByteArrayToFile(new File(dirName + SEPTOR + fileName), content);
		} else {
			LOGGER.error("Directory missing ({})", dirName);
			throw new Exception("Directory missing (" + dirName + ")");
		}
	}

	/**
	 * Suppression physique d'un fichier
	 * 
	 * @param sha1Unique
	 *            l'identifiant de stockage du fichier (aussi son empreinte unique)
	 * @return true si le fichier a été supprimé, false sinon
	 * @throws IOException
	 */
	public boolean deleteFile(String sha1Unique) throws IOException {

		String dirName = idStorage + SEPTOR + sha1Unique.substring(0, 2);
		String fileName = sha1Unique.substring(2);

		File file = new File(dirName + SEPTOR + fileName);
		if (file.exists()) {
			boolean result = false;
			RandomAccessFile raf = null;
			FileChannel channel = null;
			MappedByteBuffer buffer = null;
			SecureRandom random = new SecureRandom();
			raf = new RandomAccessFile(file, "rws");
			channel = raf.getChannel();
			buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, raf.length());

			// pass 1 : 0x01 dans tous les secteurs
			while (buffer.hasRemaining()) {
				buffer.put((byte) 0x01);
			}
			buffer.force();
			buffer.rewind();
			// pass 2 : 0x27FFFFFF
			while (buffer.hasRemaining()) {
				buffer.put((byte) 0x27FFFFFF);
			}
			buffer.force();
			buffer.rewind();
			// pass 3 : séquences de symboles aléatoires
			byte[] data = new byte[1];
			while (buffer.hasRemaining()) {
				random.nextBytes(data);
				buffer.put(data[0]);
			}
			buffer.force();
			channel.close();
			raf.close();
			// pass 4 : verification suppression
			Method cleanerMethod;
			try {
				cleanerMethod = buffer.getClass().getMethod("cleaner");
				cleanerMethod.setAccessible(true);
				Object cleaner = cleanerMethod.invoke(buffer);
				Method cleanMethod = cleaner.getClass().getMethod("clean");
				cleanMethod.setAccessible(true);
				cleanMethod.invoke(cleaner);
			} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				LOGGER.error(e.getMessage());
				return false;
			}
			result = file.delete();
			LOGGER.debug("resultat effacement : {} ", result);
			return result;
		} else {
			LOGGER.error("fichier à supprimer introuvable ");
			return false;
		}

	}

	/**
	 * Récupération du contenu d'un fichier
	 * 
	 * @param sha1Unique
	 *            identifiant de stockage du fichier à récupérer
	 * @return le contenu du fichier en bytes
	 * @throws Exception
	 */
	public byte[] getFile(String sha1Unique) throws Exception {
		String dirName = idStorage + SEPTOR + sha1Unique.substring(0, 2);
		String fileName = sha1Unique.substring(2);

		LOGGER.debug("Récupérer fichier : {}", sha1Unique);

		File file = new File(dirName + SEPTOR + fileName);
		if (file.exists()) {
			byte[] content = FileUtils.readFileToByteArray(file);
			return content;
		} else {
			LOGGER.error("fichier à récupérer introuvable ");
			throw new Exception("fichier à récupérer introuvable ");
		}

	}

	/**
	 * Verifier l'integralite d'une liste de fichiers déjà archivés
	 * 
	 * @param documents
	 *            : liste de fichiers identifiant de stockage du fichier à récupérer
	 * @return boolean : true si tous les documents sont verifies et le resultat est
	 *         bon
	 * @throws Exception
	 */
	public boolean checkFiles(List<DocumentBean> documents, Map<Long, FileReturnError> badDocs) {

		
		
		final int nbThread = Math.min(documents.size()/MAX_CHECK_FILES_BY_THREAD + 1, MAX_CHECK_FILES_THREAD);
		int offset = 0;
		
		ExecutorService executor = Executors.newFixedThreadPool(nbThread);
		List<Callable<Boolean>> tasks = new ArrayList<>();
		
		while ( offset < documents.size() ) {
			
			// Get offset view of document liste
			
			final int toIndex = (offset+MAX_CHECK_FILES_BY_THREAD > documents.size()) ? 
					documents.size() : offset+MAX_CHECK_FILES_BY_THREAD;
			List<DocumentBean> offsetList = documents.subList(offset, toIndex);
			
			// Callable funtion
			Callable<Boolean> call = () -> {
				boolean allCheckOk = true;
				for (DocumentBean c : offsetList) {

					// Récupérer le contenu crypté du document
					byte[] content;
					try {
						content = getFile(c.getEmpreinteUnique());
					} catch (Exception e) {
						allCheckOk = false;
						
						// Noter l'archive en problème et la cause. Continuer avec la prochaine
						badDocs.put(c.getDocid(), FileReturnError.NOT_FOUND_ERROR);
						
						// Logger du document en problème ; si le contrôle de l'intégralité n'est pas !
						LOGGER.error("Fichier {} introuvable ", c.getTitle() + " ID=" + c.getDocid());
						continue;
					}

					// Décrypter le contenu si besoin
					if (c.getCryptage()) {
						try {
							content = AesCipher.decrypt(c.getSecretKey(), c.getInitVector(), content);
						} catch (AesCipherException e) {
							allCheckOk = false;
							
							// Noter l'archive en problème et la cause. Continuer avec la prochaine
							badDocs.put(c.getDocid(), FileReturnError.DECRYPT_ERROR);
							
							// Logger du document en problème ; si le contrôle de l'intégralité n'est pas !
							LOGGER.error("Décryptage du fichier {} échoué ", c.getTitle() + " ID=" + c.getDocid());
							continue;
						}
					}

					// Contrôle l'intégralité du document
					String printCalculated = null;
					try {
						printCalculated = Sha.encode(
								c.getTitle() + c.getArchiveDateMs() + Base64.getEncoder().encodeToString(content), "utf-8");
					} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
						allCheckOk = false;
						
						// Noter l'archive en problème et la cause. Continuer avec la prochaine
						badDocs.put(c.getDocid(), FileReturnError.SHA_HASH_ERROR);
						
						// Logger du document en problème ; si le contrôle de l'intégralité n'est pas !
						LOGGER.error("Hachage du fichier {} échoué ", c.getTitle() + " ID=" + c.getDocid());
						continue;
					}

					if (!printCalculated.equals(c.getEmpreinteSimple())) {
						allCheckOk = false;

						LOGGER.debug("Empreinte Simple : " + c.getEmpreinteSimple());
						LOGGER.debug("Empreinte calculé : " + printCalculated);

						badDocs.put(c.getDocid(), FileReturnError.ENTIRETY_ERROR);

						// Logger du document en problème ; si le contrôle de l'intégralité n'est pas !
						LOGGER.error("Contrôle l'intégralité du fichier {} échoué ", c.getTitle() + " ID=" + c.getDocid());
					}
				}
				
				return allCheckOk;
			};

			// Liste de task a excuter
			tasks.add(call);
			LOGGER.debug("Starting of a sub thread for docs \n {} ", offsetList);
			
			offset += MAX_CHECK_FILES_BY_THREAD;
			
		}
		
		boolean allThreadOk = true;

		try {
			for (Future<Boolean> result : executor.invokeAll(tasks)) {
				if ( !result.get(CHECK_FILES_TASK_TIMEOUT, TimeUnit.MINUTES) ) {
					// Si traitement a eu probleme
					LOGGER.error("Controler de l'intégralité des archives n'est pas tout reussi sur un sub Thread ");
					allThreadOk = false;
				}
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOGGER.error("Erreur lors de l'execution des threads - interrompue ou échouée" + e.getMessage());
			allThreadOk = false;
		}

		return allThreadOk;

	}
}
