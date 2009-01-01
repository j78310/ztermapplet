/**
 * TelnetOutputStream.java
 */
package org.zhouer.protocol;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * @author Chin-Chang Yang
 */
public class TelnetOutputStream extends OutputStream {
	private final Telnet telnet;

	public TelnetOutputStream(final Telnet tel) {
		this.telnet = tel;
	}

	public void write(final byte[] buf) throws IOException {
		this.telnet.writeBytes(buf);
	}

	public void write(final byte[] buf, final int offset, final int length)
			throws IOException {
		this.telnet.writeBytes(buf, offset, length);
	}

	public void write(final int b) throws IOException {
		// java doc: The 24 high-order bits of b are ignored.
		this.telnet.writeByte((byte) b);
	}

}
