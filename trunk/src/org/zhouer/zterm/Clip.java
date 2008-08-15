package org.zhouer.zterm;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Clip manages system clip board.
 * 
 * @author h45
 */
public class Clip implements ClipboardOwner {

	/**
	 * Getter of content of system clip board
	 * 
	 * @return content.
	 */
	public String getContent() {
		final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		final Transferable contents = clip.getContents(null);

		if (contents != null) {
			if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					return (String) contents
							.getTransferData(DataFlavor.stringFlavor);
				} catch (final UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	public void lostOwnership(final Clipboard clipboard,
			final Transferable contents) {
		// No activities have to be done when this object lost owner ship.
	}

	/**
	 * Setter of content of system clip board
	 * 
	 * @param content
	 *            the clip board to be set
	 */
	public void setContent(final String content) {
		final StringSelection ss = new StringSelection(content);
		final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		clip.setContents(ss, this);
	}
}
