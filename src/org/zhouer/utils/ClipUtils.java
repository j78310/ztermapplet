package org.zhouer.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
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
public class ClipUtils {
	
	private ClipUtils() {
		// This class shouldn't be instantialized.
	}
	
	/**
	 * Getter of content of system clip board
	 * 
	 * @return content.
	 */
	public static String getContent() {
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

	/**
	 * Setter of content of system clip board
	 * 
	 * @param content
	 *            the clip board to be set
	 */
	public static void setContent(final String content) {
		final StringSelection stringSelection = new StringSelection(content);
		final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		clip.setContents(stringSelection, stringSelection);
	}
}
