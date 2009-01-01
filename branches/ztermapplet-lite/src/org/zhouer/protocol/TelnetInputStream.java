/**
 * TelnetInputStream.java
 */
package org.zhouer.protocol;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @author Chin-Chang Yang
 */
public class TelnetInputStream extends InputStream {
	private final Telnet telnet;

	public TelnetInputStream(final Telnet tel) {
		this.telnet = tel;
	}

	public int read() throws IOException {
		return this.telnet.readByte();
	}

	public int read(final byte[] buf) throws IOException {
		return this.telnet.readBytes(buf);
	}

	public int read(final byte[] buf, final int offset, final int length)
			throws IOException {
		return this.telnet.readBytes(buf, offset, length);
	}

}
