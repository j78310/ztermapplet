package org.zhouer.protocol;

import java.io.InputStream;
import java.io.OutputStream;

public interface Protocol {
	public static final String TELNET = "telnet";
	public boolean connect();
	public void disconnect();
	public InputStream getInputStream();
	public OutputStream getOutputStream();
	public String getTerminalType();
	public boolean isClosed();
	public boolean isConnected();
	public void setTerminalType(String tt);
}
