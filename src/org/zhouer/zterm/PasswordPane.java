package org.zhouer.zterm;

import java.awt.Component;
import java.awt.FlowLayout;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

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
	 * Prompts the user for input in a password dialog.
	 * 
	 * @param parentComponent the parent Component for the dialog
	 * @param message the Object to display
	 * @param title the String to display in the dialog title bar
	 * @return user's input, or null meaning the user canceled the input
	 */
	public static String showPasswordDialog(Component parentComponent, Object message, String title) {
		final PasswordPane passwordPane = new PasswordPane(message.toString()); //$NON-NLS-1$ 
		final JDialog dialog = passwordPane.createDialog(parentComponent, title); //$NON-NLS-1$

		// 建立把焦點放在密碼輸入欄位中的工作
		final Runnable focusPasswordField = new Runnable() {
			public void run() {
				passwordPane.passField.requestFocusInWindow();
			}
		};
		
		// 建立顯示對話方塊的動作
		final Runnable showDialog = new Runnable() {
			public void run() {
				// 要求等會兒把焦點放在密碼輸入欄位上面
				SwingUtilities.invokeLater(focusPasswordField);
				dialog.setVisible(true);
			}
		};

		try {
			// 要求顯示對話方塊並且等待
			SwingUtilities.invokeAndWait(showDialog);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		if (passwordPane.getValue() != null) {
			if (passwordPane.getValue() instanceof Integer) {
				if (passwordPane.getValue().equals(
						new Integer(JOptionPane.OK_OPTION))) {

					// 取得密碼並且回傳
					return passwordPane.getPassword();
				}
			}
		}

		// 使用者取消的時候回傳 null
		return null;
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
