package org.zhouer.utils;

/**
 * Recognize URL string.
 * 
 * @author Chin-Chang Yang
 */
public class UrlRecognizer {

	/**
	 * Detect a position of a message is a part of HTTP. For example, message =
	 * "Books, and Flowers: http://123456789", index = 5, it should return
	 * false. message = "Books, and Flowers: http://123456789", index = 30, it
	 * should return true.
	 * 
	 * @param message
	 *            message that contains HTTP
	 * @param index
	 *            index of message to be detected whether it is part of HTTP
	 * @return true, if the character corresponding to the index in the massage
	 *         is a part of HTTP; false, otherwise.
	 */
	public static boolean isPartOfHttp(final String message, final int index) {
		// 1. message: null 2. message: "words" 3. message: "http://test http://test" 4. message: "http://test/測試"  5. message: "http://test" 
		if (message == null) {
			return false;
		}

		if (index < 0 || index >= message.length()) {
			throw new IllegalArgumentException("Out of bound!");
		}

		final int lastIndexOfHttp = message.lastIndexOf("http://", index);
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
		final int indexOfQuates = httpMessage.indexOf('\"');
		final boolean emptyNotFound = indexOfEmpty == -1;
		final boolean spaceNotFound = indexOfSpace == -1;
		final boolean tNotFound = indexOft == -1;
		final boolean nNotFound = indexOfn == -1;
		final boolean rNotFound = indexOfr == -1;
		final boolean fNotFound = indexOff == -1;
		final boolean quatesNotFound = indexOfQuates == -1;

		// 1. message: "http://test http://test" 2. message: "http://test" 
		if (emptyNotFound && spaceNotFound && tNotFound && nNotFound
				&& rNotFound && fNotFound && quatesNotFound) {
			return true;
		}

		// 1. message: "http://test http://test"
		return false;
	}
}
