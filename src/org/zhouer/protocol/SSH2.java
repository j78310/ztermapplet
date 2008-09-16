package org.zhouer.protocol;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;

import org.zhouer.zterm.model.Model;

import ch.ethz.ssh2.InteractiveCallback;

public class SSH2 implements Protocol {
	private boolean authenticated;
	private ch.ethz.ssh2.Connection conn;
	private boolean connected, closed;

	private final String host;
	private InputStream is;

	private OutputStream os;
	private final int port;

	private ch.ethz.ssh2.Session sess;
	private String terminalType;

	public SSH2(final String h, final int p) {
		this.host = h;
		this.port = p;

		this.connected = false;
		this.authenticated = false;
		this.closed = false;
	}

	public boolean connect() {
		try {
			// System.out.println("SSH2 to host: " + host + ", port: " + port +
			// " ..." );
			this.conn = new ch.ethz.ssh2.Connection(this.host, this.port);
			// FIXME: magic number, 應可自行設定 timeout
			this.conn.connect(null, 5000, 60000);
			this.conn.setTCPNoDelay(true);
			this.connected = true;

			final String username = Auth.getUsername();
			if (username == null) {
				this.disconnect();
				return false;
			}

			final String[] methods = this.conn
					.getRemainingAuthMethods(username);
			if (methods.length == 0) {
				// this.authenticated = this.conn.authenticateWithNone(username);
				// System.out.println( "SSH2: no authentication is needed" );
			} else {
				// for(int i = 0; i < methods.length; i++) {
				// System.out.println( methods[i] );
				// }
				if (this.conn.isAuthMethodAvailable(username, "password")) {
					final String password = Auth.getPassword();
					if (password != null) {
						this.authenticated = this.conn
								.authenticateWithPassword(username, password);
					}
					// System.out.println("SSH2: password auth");
				} else if (this.conn.isAuthMethodAvailable(username,
						"keyboard-interactive")) {
					this.authenticated = this.conn
							.authenticateWithKeyboardInteractive(username,
									new Auth());
					// System.out.println("SSH2: keyboard-interactive auth");
				} else if (this.conn.isAuthMethodAvailable(username,
						"publickey")) {
					System.out.println("SSH2 publickey(not yet support)");
				} else {
					System.out.println("unknown SSH2 authentication method.");
				}
			}

			if (this.authenticated == false) {
				// System.out.println("authentication failed.");
				this.disconnect();
				return false;
			} else {
				// System.out.println("authentication success.");
			}

			this.sess = this.conn.openSession();
			this.sess.requestPTY(this.terminalType, 80, 24, 0, 0, null);
			this.sess.startShell();
			this.is = this.sess.getStdout();
			this.os = this.sess.getStdin();

			return true;

		} catch (final IOException e) {
			// e.printStackTrace();
			System.out.println("Caught IOException in SSH2::connect()");
			this.connected = false;
			this.disconnect();
			return false;
		}
	}

	public void disconnect() {
		// 身份認證成功則需要 close
		if (this.authenticated) {
			try {
				this.is.close();
				this.os.close();
				this.sess.close();
			} catch (final IOException e) {
				// e.printStackTrace();
				System.out.println("Caught IOException in SSH2::disconnect()");
			}
		}

		// 不論是否連線成功都要 close
		this.conn.close();

		this.closed = true;
	}

	public InputStream getInputStream() {
		return new SSH2InputStream(this);
	}

	public OutputStream getOutputStream() {
		return new SSH2OutputStream(this);
	}

	public String getTerminalType() {
		return this.terminalType;
	}

	public boolean isClosed() {
		return this.closed;
	}

	public boolean isConnected() {
		return this.connected;
	}

	public int readByte() throws IOException {
		int r;

		r = this.is.read();
		if (r == -1) {
			// System.out.println("read -1 (EOF), disconnect().");
			throw new IOException();
		}
		return r;
	}

	public int readBytes(final byte[] b) throws IOException {
		int r;

		r = this.is.read(b);

		if (r == -1) {
			// System.out.println("read -1 (EOF), disconnect().");
			throw new IOException();
		}

		return r;
	}

	public int readBytes(final byte[] b, final int offset, final int length)
			throws IOException {
		int r;

		r = this.is.read(b, offset, length);

		if (r == -1) {
			// System.out.println("read -1 (EOF), disconnect().");
			throw new IOException();
		}

		return r;
	}

	public void setTerminalType(final String tt) {
		// TODO: 現在還不能動態改變，只有連線前就設定好才有用。
		this.terminalType = tt;
	}

	public void writeByte(final byte b) throws IOException {
		this.os.write(b);
		this.os.flush();
	}

	public void writeBytes(final byte[] buf) throws IOException {
		this.os.write(buf);
		this.os.flush();
	}

	public void writeBytes(final byte[] buf, final int offset, final int size)
			throws IOException {
		this.os.write(buf, offset, size);
		this.os.flush();
	}
}

class Auth implements InteractiveCallback {

	public static String getPassword() {
		return Model.getInstance().getPassword();
	}

	public static String getUsername() {
		return Model.getInstance().getUsername();
	}

	public String[] replyToChallenge(final String name,
			final String instruction, final int numPrompts,
			final String[] prompt, final boolean[] echo) throws Exception {
		/*
		 * final String[] ret = new String[numPrompts]; //
		 * System.out.println("name: " + name ); //
		 * System.out.println("instruction: " + instruction );
		 * 
		 * for (int i = 0; i < numPrompts; i++) { if (echo[i]) { ret[i] =
		 * JOptionPane.showInputDialog(null, prompt[i], "Challenge",
		 * JOptionPane.QUESTION_MESSAGE); } else { ret[i] =
		 * Auth.getPassword("Challenge", prompt[i]); } }
		 * 
		 * return ret;
		 */
		return null;
	}
}

class PasswordDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 475389458121763833L;

	JPasswordField passField;
	JLabel passLabel;

	public PasswordDialog(final Frame owner, final String title,
			final String prompt, final boolean modal) {
		super(owner, title, modal);

		this.passLabel = new JLabel(prompt);
		this.passField = new JPasswordField(10);
		this.passField.addActionListener(this);

		this.getContentPane().setLayout(new FlowLayout());
		this.getContentPane().add(this.passLabel);
		this.getContentPane().add(this.passField);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == this.passField) {
			this.dispose();
		}
	}

	public String getPassword() {
		return new String(this.passField.getPassword());
	}
}

class SSH2InputStream extends InputStream {
	private final SSH2 ssh2;

	public SSH2InputStream(final SSH2 s) {
		this.ssh2 = s;
	}

	public int read() throws IOException {
		return this.ssh2.readByte();
	}

	public int read(final byte[] buf) throws IOException {
		return this.ssh2.readBytes(buf);
	}

	public int read(final byte[] buf, final int offset, final int length)
			throws IOException {
		return this.ssh2.readBytes(buf, offset, length);
	}
}

class SSH2OutputStream extends OutputStream {
	private final SSH2 ssh2;

	public SSH2OutputStream(final SSH2 s) {
		this.ssh2 = s;
	}

	public void write(final byte[] b) throws IOException {
		this.ssh2.writeBytes(b);
	}

	public void write(final byte[] b, final int offset, final int length)
			throws IOException {
		this.ssh2.writeBytes(b, offset, length);
	}

	public void write(final int b) throws IOException {
		this.ssh2.writeByte((byte) b);
	}
}
