package org.zhouer.zterm;

import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * HtmlDialog is a dialog to show a html page with a url.
 * 
 * @author h45
 */
public class HtmlPane extends JOptionPane {
	private static final long serialVersionUID = -2801813211379571475L;

	/**
	 * Constructor of HtmlDialog with a url
	 * 
	 * @param url
	 *            to be read and show on this dialog
	 */
	public HtmlPane(final URL url) {
		try {
			final JEditorPane htmlPane = new JEditorPane(url);
			htmlPane.setEditable(false);
			this.setMessage(new JScrollPane(htmlPane));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
