package org.zhouer.utils;


/**
 * Recognize URL string.
 *
 * @author Chin-Chang Yang
 */
public class UrlRecognizer {

	/**
	 * Detect a position of a message is a part of url.
	 * 
	 * @param message message that contains url
	 * @param index index of message to be detected whether it is part of url
	 * @return true, if the character corresponding to the index in the massage is a part of url; false, otherwise. 
	 */
	public static boolean isPartOfUrl(final String message, final int index) {
		if (message == null) {
			return false;
		}
		
		if (index < 0 || index >= message.length()) {
			throw new IllegalArgumentException("Out of bound!");
		}
		
		final String subMessage = message.substring(0, index + 1);
		final int lastIndexOfHttp = subMessage.lastIndexOf("http://");
		final boolean httpNotFound = lastIndexOfHttp == -1;
		
		if (httpNotFound) {
			return false;
		}
		
		final String httpMessage = subMessage.substring(lastIndexOfHttp);
		final int indexOfSpace = httpMessage.indexOf(' ');
		final int indexOft = httpMessage.indexOf(' ');
		final int indexOfn = httpMessage.indexOf('\n');
		final int indexOfr = httpMessage.indexOf('\r');
		final int indexOff = httpMessage.indexOf('\f');
		final boolean spaceNotFound = indexOfSpace == -1;
		final boolean tNotFound = indexOft == -1;
		final boolean nNotFound = indexOfn == -1;
		final boolean rNotFound = indexOfr == -1;
		final boolean fNotFound = indexOff == -1;
		
		if (spaceNotFound && tNotFound && nNotFound && rNotFound && fNotFound) {
			return true;
		}
		
		return false;
	}
}
