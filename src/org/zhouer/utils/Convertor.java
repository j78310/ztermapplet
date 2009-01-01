package org.zhouer.utils;

import java.io.IOException;
import java.io.InputStream;

public class Convertor {
	
	public static boolean isValidBig5(final byte[] b, final int offset,
			final int limit) {
		// TODO: 改為較嚴謹的 Big5 規格
		if (b[offset] >= 0) {
			return limit == 1;
		} else {
			return limit == 2;
		}
	}

	public static boolean isValidUTF8(final byte[] b, final int offset,
			final int limit) {
		// TODO: 改為較嚴謹的 UTF-8 規格
		if (b[offset] >= 0) {
			return limit == 1;
		} else if ((b[offset] >= -64) && (b[offset] <= -33)) {
			return limit == 2;
		} else if ((b[offset] >= -32) && (b[offset] <= -17)) {
			return limit == 3;
		} else if ((b[offset] >= -16) && (b[offset] <= -9)) {
			return limit == 4;
		} else {
			// 不是合法的 UTF-8
			return true;
		}
	}

	public static boolean isWideChar(final char c) {
		// TODO: 應該改用更好的寫法判斷是否為寬字元，暫時假設非 ASCII 字元皆為寬字元。
		if (c > 127) {
			return true;
		}
		return false;
	}
	
	/**
	 * Check if a message contains any wide characters.
	 * 
	 * @param message the message to be checked
	 * @return true if the message contains wide characters; false, otherwise.
	 */
	public static boolean containsWideChar(String message) {
		for (int i = 0; i < message.length(); i++) {
			final char c = message.charAt(i);
			if (Convertor.isWideChar(c)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * 把 jar 中的檔案讀進 byte array
	 * 
	 * @param name
	 *            檔名
	 * @param b
	 *            目地 array
	 * @return 檔案長度
	 */
	public static int readFile(final String name, final byte[] b) {
		int size = 0, len;
		final InputStream is = Convertor.class.getResourceAsStream(name);

		try {
			// 本來應該 is.read( b ) 就可以才對，
			// 但是我發現在包裝成 jar 以後就會讀不完整，一定要這樣讀。
			while (true) {
				len = is.read(b, size, b.length - size);
				if (len == -1) {
					break;
				}
				size += len;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return size;
	}

	private final byte[] big5bytes;
	private final byte[] ucs2bytes;
	private final char[] ucs2chars;

	private volatile static Convertor convertor = null;

	/**
	 * Getter of instance in Singleton pattern
	 * 
	 * @return singleton instance of model.
	 */
	public static Convertor getInstance() {
		if (Convertor.convertor == null) {
			synchronized (Convertor.class) {
				if (Convertor.convertor == null) {
					Convertor.convertor = new Convertor();
				}
			}
		}

		return Convertor.convertor;
	}
	
	private Convertor() {
		int i1, i2;

		this.ucs2bytes = new byte[64 * 1024];
		this.big5bytes = new byte[128 * 1024];

		Convertor.readFile("conv/ucs2.txt", this.ucs2bytes);
		Convertor.readFile("conv/big5.txt", this.big5bytes);

		this.ucs2chars = new char[this.ucs2bytes.length / 2];

		// 把讀進來的 ucs2 bytes 處理成標準的 (ucs2) char
		for (int i = 0; i < this.ucs2bytes.length; i += 2) {
			i1 = (this.ucs2bytes[i] < 0 ? 256 : 0) + this.ucs2bytes[i];
			i2 = (this.ucs2bytes[i + 1] < 0 ? 256 : 0) + this.ucs2bytes[i + 1];
			this.ucs2chars[i / 2] = (char) (i1 << 8 | i2);
		}
	}

	public char big5BytesToChar(final byte[] buf, final int offset,
			final int limit) {
		if (limit == 1) {
			return (char) buf[offset];
		}

		// signed to unsigned
		int i1 = (buf[offset] < 0 ? 256 : 0) + buf[offset];
		int i2 = (buf[offset + 1] < 0 ? 256 : 0) + buf[offset + 1];

		// 表是從 big5 0x8140 開始建的
		final int shift = ((i1 << 8) | i2) - 0x8140;

		// 超過 big5 的範圍
		if ((shift < 0) || (shift * 2 + 1 >= this.ucs2bytes.length)) {
			return '?';
		}

		i1 = (this.ucs2bytes[shift * 2] < 0 ? 256 : 0)
				+ this.ucs2bytes[shift * 2];
		i2 = (this.ucs2bytes[shift * 2 + 1] < 0 ? 256 : 0)
				+ this.ucs2bytes[shift * 2 + 1];

		return (char) (i1 << 8 | i2);
	}

	public String big5BytesToString(final byte[] buf, final int offset,
			final int limit) {
		final StringBuffer sb = new StringBuffer();

		for (int i = 0; i < limit; i++) {
			if ((i + 1 < limit) && (buf[offset + i] < 0)) {
				sb.append(this.big5BytesToChar(buf, offset + i, 2));
				i += 1;
			} else {
				sb.append((char) buf[offset + i]);
			}
		}

		return new String(sb);
	}

	public char bytesToChar(final byte[] b, final int from, final int limit,
			final String encoding) {
		// FIXME: magic number
		if (encoding.equalsIgnoreCase("Big5")) {
			return this.big5BytesToChar(b, from, limit);
		} else if (encoding.equalsIgnoreCase("UTF-8")) {
			return this.utf8BytesToChar(b, from, limit);
		} else {
			// TODO: 其他的編碼
			System.out.println("Unknown Encoding: " + encoding);
			return 0;
		}
	}

	public byte[] charToBig5Bytes(final char c) {
		byte[] b;

		// 假設非 ASCII 都是 2 bytes
		if (c < 0x80) {
			b = new byte[1];
			b[0] = (byte) c;
			return b;
		} else {
			b = new byte[2];
			// 表是從 Unicode 0x80 開始建的
			b[0] = this.big5bytes[(c - 0x80) * 2];
			b[1] = this.big5bytes[(c - 0x80) * 2 + 1];
		}

		return b;
	}

	public byte[] charToBytes(final char c, final String encoding) {
		if (encoding.equalsIgnoreCase("Big5")) {
			return this.charToBig5Bytes(c);
		} else if (encoding.equalsIgnoreCase("UTF-8")) {
			return this.charToUTF8Bytes(c);
		} else {
			// TODO: 其他的編碼
			System.out.println("Unknown Encoding: " + encoding);
			return null;
		}
	}

	public byte[] charToUTF8Bytes(final char c) {
		byte[] b;

		if ((c >= 0) && (c <= 0x7f)) {
			b = new byte[1];
			b[0] = (byte) c;
		} else if ((c >= 0x80) && (c <= 0x7ff)) {
			b = new byte[2];
			b[0] = (byte) (0xc0 | (c >> 6));
			b[1] = (byte) (0x80 | (c & 0x3f));
		} else if ((c >= 0x800) && (c <= 0xffff)) {
			b = new byte[3];
			b[0] = (byte) (0xe0 | (c >> 12));
			b[1] = (byte) (0x80 | ((c >> 6) & 0x3f));
			b[2] = (byte) (0x80 | (c & 0x3f));
		} else if ((c >= 0x10000) && (c <= 0x10ffff)) {
			b = new byte[4];
			b[0] = (byte) (0xf0 | (c >> 18));
			b[1] = (byte) (0x80 | ((c >> 12) & 0x3f));
			b[2] = (byte) (0x80 | ((c >> 6) & 0x3f));
			b[3] = (byte) (0x80 | (c & 0x3f));
		} else {
			System.out.println("Error converting char to UTF-8 bytes.");
			b = null;
		}

		return b;
	}

	public boolean isValidMultiBytes(final byte[] b, final int from,
			final int limit, final String encoding) {
		// FIXME: magic number
		if (encoding.equalsIgnoreCase("Big5")) {
			return Convertor.isValidBig5(b, from, limit);
		} else if (encoding.equalsIgnoreCase("UTF-8")) {
			return Convertor.isValidUTF8(b, from, limit);
		} else {
			// TODO: 其他的編碼
			System.out.println("Unknown Encoding: " + encoding);
			return true;
		}
	}

	public byte[] StringToBig5Bytes(final String str) {
		int count = 0;
		byte[] buf;
		final byte[] tmp = new byte[str.length() * 2];
		byte[] result;

		for (int i = 0; i < str.length(); i++) {
			buf = this.charToBig5Bytes(str.charAt(i));
			for (int j = 0; j < buf.length; j++) {
				tmp[count++] = buf[j];
			}
		}

		result = new byte[count];
		for (int i = 0; i < count; i++) {
			result[i] = tmp[i];
		}

		return result;
	}

	public char utf8BytesToChar(final byte[] buf, final int offset,
			final int limit) {
		char c;

		if (limit == 1) {
			c = (char) buf[0];
		} else if (limit == 2) {
			c = (char) (buf[0] & 0x1f);
			c <<= 6;
			c |= (char) (buf[1] & 0x3f);
		} else if (limit == 3) {
			c = (char) (buf[0] & 0xf);
			c <<= 6;
			c |= (char) (buf[1] & 0x3f);
			c <<= 6;
			c |= (char) (buf[2] & 0x3f);
		} else {
			c = (char) (buf[0] & 0x7);
			c <<= 6;
			c |= (char) (buf[1] & 0x3f);
			c <<= 6;
			c |= (char) (buf[2] & 0x3f);
			c <<= 6;
			c |= (char) (buf[3] & 0x3f);
		}

		return c;
	}
}
