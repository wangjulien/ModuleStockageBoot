package com.telino.modulestockage.util;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Un singleton chargeur du fichier de configuration "config.properties", qui se trouve dans ClassPath
 * 
 * @author Jiliang.WANG
 *
 */
public enum ConfigFile {
	PROPERTIES;

	private final static String FILE_NAME = "/config.properties";
	private Properties properties = new Properties();

	private ConfigFile() {
		load();
	}

	private void load() {
		try (InputStream input = ConfigFile.class.getResourceAsStream(FILE_NAME)) {
			properties.load(input);
		} catch (Exception e) {
			// Logger exception. 
			Logger LOGGER = LoggerFactory.getLogger(ConfigFile.class);
			LOGGER.error(e.toString());
			
			// Lever une Runtime, car si config non chargé, il faut que module se plante
			throw new RuntimeException(e);
		}
	}

	public String get(String key) {
		return properties.getProperty(key);
	}
}
