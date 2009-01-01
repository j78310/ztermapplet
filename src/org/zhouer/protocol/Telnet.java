package org.zhouer.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Telnet implements Protocol {
	public final static byte DO = (byte) 253;
	public final static byte DONT = (byte) 254;

	public final static byte ECHO = 1; // Echo
	public final static byte IAC = (byte) 255;
	public final static byte NOP = (byte) 241;

	// public final static byte DM = (byte)242;
	// public final static byte BRK = (byte)243;
	// public final static byte IP = (byte)244;
	// public final static byte AO = (byte)245;
	// public final static byte AYT = (byte)246;
	// public final static byte EC = (byte)247;
	// public final static byte EL = (byte)248;
	// public final static byte GA = (byte)249;
	public final static byte SB = (byte) 250;

	public final static byte SE = (byte) 240;
	public final static byte SGA = 3; // Supress Go Ahead
	public final static byte TS = 32; // Terminal Speed
	// public final static byte RFC = 33; // Remote Flow Control
	// public final static byte LM = 34; // Linemode
	// public final static byte XDL = 35; // X Display Location
	// public final static byte EV = 36; // Environment Variables
	// public final static byte NEO = 39; // New Environment Option
	// public final static byte ST = 5; // Status
	// public final static byte TM = 6; // Terminal Mark
	public final static byte TT = 24; // Terminal Type
	public final static byte WILL = (byte) 251;

	public final static byte WONT = (byte) 252;
	public final static byte WS = 31; // Window Size
	
	// 從下層來的資料暫存起來
	private final byte[] buf;
	private int bufpos, buflen;
	private final String host;
	private InputStream is;
	private OutputStream os;
	private final int port;

	private Socket sock;
	private String terminal_type;

	public Telnet(final String host, final int port) {
		this.host = host;
		this.port = port;

		// 預設的 terminal type 是 vt100
		this.terminal_type = "vt100";

		// 使用 buffer
		this.buf = new byte[4096];
		this.bufpos = this.buflen = 0;
	}

	public boolean connect() {
		try {
			this.sock = new Socket(this.host, this.port);

			// disable Nagle's algorithm
			// 不要把很多小封包包裝成一個大封包再一次送出，
			// 這非常重要，連續顯示的游標是使用者評斷顯示速度的重要依據。
			this.sock.setTcpNoDelay(true);

			// 設定 keep alive
			this.sock.setKeepAlive(true);

			this.is = this.sock.getInputStream();
			this.os = this.sock.getOutputStream();
		} catch (final UnknownHostException e) {
			// 可能是未連線或連線位置錯誤
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void disconnect() {
		// 如果根本沒連線成功或是連線已被關閉則不做任何事。
		if ((this.sock == null) || this.sock.isClosed()) {
			return;
		}

		try {
			this.is.close();
			this.os.close();
			this.sock.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		if (!this.sock.isClosed()) {
			System.err.println("Disconnect failed!");
		}
	}

	public InputStream getInputStream() {
		return new TelnetInputStream(this);
	}

	public OutputStream getOutputStream() {
		return new TelnetOutputStream(this);
	}

	public String getTerminalType() {
		return this.terminal_type;
	}

	public boolean isClosed() {
		// XXX: 連線建立失敗也當成已關閉連線。
		if (this.sock == null) {
			return true;
		}

		return this.sock.isClosed();
	}

	public boolean isConnected() {
		if (this.sock == null) {
			return false;
		}
		return this.sock.isConnected();
	}

	public int readByte() throws IOException {
		return this.read();
	}

	public int readBytes(final byte[] b) throws IOException {
		return this.readBytes(b, 0, b.length);
	}

	public int readBytes(final byte[] b, final int offset, final int length)
			throws IOException {
		int len = 0;

		while (true) {

			// 把 buf 都用完了，但還沒有裝東西到 b 去，就把 buf 重新裝滿。
			if ((this.bufpos == this.buflen) && (len == 0)) {
				// 會從這邊拿到 IOException
				this.fillBuf();
				continue;
			}

			// 當 buf 用完了，或是裝了 length bytes 後就結束。
			if ((this.bufpos == this.buflen) || (len == length)) {
				break;
			}

			if (this.buf[this.bufpos] == Telnet.IAC) {
				// 把控制字元處理掉
				this.bufpos++;
				this.proc_iac();
			} else {
				// 把 buf 的資料裝進 b
				b[offset + len++] = this.buf[this.bufpos++];
			}
		}

		return len;
	}

	public void setTerminalType(final String tt) {
		// TODO: 現在還不能動態改變，只有連線前就設定好才有用。
		this.terminal_type = tt;
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

	private void fillBuf() throws IOException {
		this.buflen = this.is.read(this.buf);
		if (this.buflen == -1) {
			throw new IOException();
		}
		this.bufpos = 0;
	}

	private void proc_do() throws IOException {
		byte b;

		b = this.read();
		// System.out.println("Do " + b );
		if (b == Telnet.WS) {
			this.send_command(Telnet.WILL, b);
			// FIXME: Magic number (80x24)
			final byte[] ws = { 0x50, 0x00, 0x18 };
			this.send_sb(b, (byte) 0, ws, 3);
		} else if ((b == Telnet.TT) || (b == Telnet.TS)) {
			this.send_command(Telnet.WILL, b);
		} else if (b == Telnet.ECHO) {
			// XXX: 為什麼？
			this.send_command(Telnet.WONT, b);
		} else {
			this.send_command(Telnet.WONT, b);
		}
	}

	private void proc_dont() throws IOException {
		byte b;

		b = this.read();
		// WONT is the only valid responce.
		this.send_command(Telnet.WONT, b);
	}

	private void proc_iac() throws IOException {
		byte b;

		b = this.read();
		switch (b) {
		case SB:
			this.proc_sb();
			break;
		case WILL:
			this.proc_will();
			break;
		case WONT:
			this.proc_wont();
			break;
		case DO:
			this.proc_do();
			break;
		case DONT:
			this.proc_dont();
			break;
		default:
			break;
		}
	}

	private void proc_sb() throws IOException {
		byte b, buf1, buf2;

		b = this.read();
		buf2 = this.read();

		while (true) {
			buf1 = buf2;
			buf2 = this.read();
			if ((buf1 == Telnet.IAC) && (buf2 == Telnet.SE)) {
				break;
			}
		}

		switch (b) {
		case TT:
			final byte[] ttbuf = this.terminal_type.getBytes();
			this.send_sb(b, (byte) 0, ttbuf, ttbuf.length);
			break;
		case TS:
			// FIXME: magic number
			final byte[] tsbuf = { '3', '8', '4', '0', '0', ',', '3', '8', '4',
					'0', '0' };
			this.send_sb(b, (byte) 0, tsbuf, tsbuf.length);
			break;
		default:
			break;
		}
	}

	private void proc_will() throws IOException {
		byte b;

		b = this.read();
		// System.out.println("Will " + b );

		if ((b == Telnet.ECHO) || (b == Telnet.SGA)) {
			this.send_command(Telnet.DO, b);
		} else {
			this.send_command(Telnet.DONT, b);
		}

	}

	private void proc_wont() throws IOException {
		byte b;

		b = this.read();
		// DONT is the only valid response.
		this.send_command(Telnet.DONT, b);
	}

	private byte read() throws IOException {
		// cache 用完了，再跟下層要一次。
		while (this.bufpos == this.buflen) {
			this.fillBuf();
		}

		return this.buf[this.bufpos++];
	}

	private void send_command(final byte comm, final byte opt)
			throws IOException {
		final byte[] buf = new byte[3];

		buf[0] = Telnet.IAC;
		buf[1] = comm;
		buf[2] = opt;

		this.writeBytes(buf, 0, 3);
	}

	private void send_sb(final byte opt1, final byte opt2, final byte[] opt3,
			final int size) throws IOException {
		int i;
		final byte[] buf = new byte[size + 6];

		buf[0] = Telnet.IAC;
		buf[1] = Telnet.SB;
		buf[2] = opt1;
		buf[3] = opt2;

		for (i = 0; i < size; i++) {
			buf[4 + i] = opt3[i];
		}

		buf[4 + size] = Telnet.IAC;
		buf[5 + size] = Telnet.SE;

		this.writeBytes(buf, 0, size + 6);
	}
}