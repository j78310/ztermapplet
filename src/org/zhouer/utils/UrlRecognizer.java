package org.zhouer.utils;


/**
 * Recognize URL string.
 *
 * @author Chin-Chang Yang
 */
public class UrlRecognizer {

	private UrlRecognizer() {
		// This class shouldn't be instanced.
	}
	
	/**
	 * Detect a position of a message is a part of the protocol.
	 * @param protocol the protocol to check
	 * @param message message that contains the protocol
	 * @param index index of message to be detected whether it is part of the protocol
	 * @return true, if the character corresponding to the index in the massage is a part of the protocol; false, otherwise. 
	 */
	public static boolean partialMatch(final String protocol, final String message, final int index) {
		// 1. message: null 2. message: "words" 3. message: "http://test http://test" 4. message: "http://test/測試"  5. message: "http://test" 
		if (message == null) {
			return false;
		}
		
		if (index < 0 || index >= message.length()) {
			throw new IllegalArgumentException("Out of bound!");
		}
		
		final int lastIndexOfHttp = message.lastIndexOf(protocol + "://", index);
		final boolean httpNotFound = lastIndexOfHttp == -1;

		// 1. message: "words" 2. message: "http://test http://test" 3. message: "http://test/測試"  4. message: "http://test" 
		if (httpNotFound) {
			return false;
		}
		
		final String httpMessage = message.substring(lastIndexOfHttp, index + 1);
		
		// 1. message: "http://test http://test" 2. message: "http://test/測試" 3. message: "http://test" 
		if (Convertor.containsWideChar(httpMessage)) {
			return false;
		}
		
		final int indexOfEmpty = httpMessage.indexOf(0);
		final int indexOfSpace = httpMessage.indexOf(' ');
		final int indexOft = httpMessage.indexOf('\t');
		final int indexOfn = httpMessage.indexOf('\n');
		final int indexOfr = httpMessage.indexOf('\r');
		final int indexOff = httpMessage.indexOf('\f');
		final boolean emptyNotFound = indexOfEmpty == -1;
		final boolean spaceNotFound = indexOfSpace == -1;
		final boolean tNotFound = indexOft == -1;
		final boolean nNotFound = indexOfn == -1;
		final boolean rNotFound = indexOfr == -1;
		final boolean fNotFound = indexOff == -1;
		
		// 1. message: "http://test http://test" 2. message: "http://test" 
		if (emptyNotFound && spaceNotFound && tNotFound && nNotFound && rNotFound && fNotFound) {
			return true;
		}
		
		// 1. message: "http://test http://test"
		return false;
	}
	
	/**
	 * Detect a position of a message is a part of HTTP.
	 * 
	 * For example, message = "Books, and Flowers: http://123456789", index = 5,
	 * it should return false.
	 * message = "Books, and Flowers: http://123456789", index = 30,
	 * it should return true.
	 * 
	 * @param message message that contains HTTP
	 * @param index index of message to be detected whether it is part of HTTP
	 * @return true, if the character corresponding to the index in the massage is a part of HTTP; false, otherwise. 
	 */
	public static boolean isPartOfHttp(final String message, final int index) {
		return partialMatch("http", message, index);
	}
	
	/**
	 * Detect a position of a message is a part of FTP.
	 * @param message message that contains FTP
	 * @param index index of message to be detected whether it is part of FTP
	 * @return true, if the character corresponding to the index in the massage is a part of FTP; false, otherwise. 
	 */
	public static boolean isPartOfFtp(final String message, final int index) {
		return partialMatch("ftp", message, index);
	}
	
	/**
	 * Detect a position of a message is a part of HTTPS.
	 * 
	 * @param message message that contains HTTPS
	 * @param index index of message to be detected whether it is part of HTTPS
	 * @return true, if the character corresponding to the index in the massage is a part of HTTPS; false, otherwise. 
	 */
	public static boolean isPartOfHttps(final String message, final int index) {
		return partialMatch("https", message, index);
	}
}
