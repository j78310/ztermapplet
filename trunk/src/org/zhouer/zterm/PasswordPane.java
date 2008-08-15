package org.zhouer.zterm;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * PasswordDialog is a dialog which asks user for passwords.
 * 
 * @author h45
 */
public class PasswordPane extends JOptionPane {
	private static final long serialVersionUID = 475389458121763833L;

	protected JPasswordField passField;
	protected JLabel passLabel;

	/**
	 * Constructor with a prompt
	 * 
	 * @param prompt
	 *            message showed on the panel
	 */
	public PasswordPane(final String prompt) {
		this.setOptionType(JOptionPane.DEFAULT_OPTION);
		final JPanel panel = new JPanel();
		this.passLabel = new JLabel(prompt);
		this.passField = new JPasswordField(10);
		
		panel.setLayout(new FlowLayout());
		panel.add(this.passLabel);
		panel.add(this.passField);

		this.setMessage(panel);
	}

	/**
	 * Getter of password
	 * 
	 * @return password.
	 */
	public String getPassword() {
		return String.valueOf(this.passField.getPassword());
	}
}
