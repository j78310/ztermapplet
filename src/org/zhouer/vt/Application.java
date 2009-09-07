package org.zhouer.vt;

import java.awt.Dimension;

public interface Application {
	public void bell();

	public void colorCopy();

	public void colorPaste();

	public void copy();

	public Dimension getSize();

	public boolean isClosed();

	public boolean isConnected();

	public boolean isTabForeground();

	public void openExternalBrowser(String url);

	public void paste();

	public int readBytes(byte[] buf);

	public void scroll(int lines);

	public void setIconName(String name);

	public void setWindowTitle(String title);

	public void showMessage(String msg);

	public void showPopup(int x, int y);

	public void writeByte(byte b);

	public void writeBytes(byte[] buf, int offset, int len);

	public void writeChar(char c);

	public void writeChars(char[] buf, int offset, int len);
}
