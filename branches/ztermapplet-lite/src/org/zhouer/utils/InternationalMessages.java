package org.zhouer.utils;

import java.io.UnsupportedEncodingException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * International messages is created automatically by Eclipse used to manage internal strings
 * in source code.
 * 
 * @author h45
 */
public class InternationalMessages {
	
	private static final String BUNDLE_NAME = "res.lang.messages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(InternationalMessages.BUNDLE_NAME);

	public static void restartBundle() {
		InternationalMessages.RESOURCE_BUNDLE = ResourceBundle
				.getBundle(InternationalMessages.BUNDLE_NAME);
	}

	/**
	 * Getter of message from an external file
	 * 
	 * @param key
	 *            to be searched for
	 * @return message
	 */
	public static String getString(final String key) {
		try {
			return new String(InternationalMessages.RESOURCE_BUNDLE.getString(key).getBytes(
					"ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (final MissingResourceException e) {
			e.printStackTrace();
		}

		return '!' + key + '!';
	}

	private InternationalMessages() {
		// This class shouldn't be instanced.
	}
}
