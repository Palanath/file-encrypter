package pala.tools.fenc;

import pala.libs.generic.parsers.cli.CLIParams;

public class Options {
	private final String key;
	private final boolean decryptionMode, suppressSuccessMessages, hashMode;
	private final int bufferSize;

	public Options(CLIParams params) {
		key = params.readString((String) null, "-k", "--key");
		decryptionMode = params.checkFlag(false, "--dec", "--decrypt", "-d");
		bufferSize = params.readInt(65536, "--buffer-size", "-bs");
		suppressSuccessMessages = params.checkFlag(false, "--quiet", "-q", "--suppress-success-messages", "-s");
		hashMode = params.checkFlag(false, "-h", "--hash");
		if (hashMode && (key != null || decryptionMode))
			throw new IllegalArgumentException("Hash mode cannot be used with a --key or with --decrypt enabled.");
	}

	/**
	 * Determines whether the program is in hash mode. In hash mode, the program
	 * only hashes all the files it encounters; it does not encrypt or decrypt
	 * anything. This option cannot be used with {@link #isDecryptionMode()
	 * decryption mode}. When this option is specified, a {@link #getKey() key}
	 * should not be specified. When this option is specified, other options are
	 * ignored.
	 * 
	 * @return Whether hash mode is enabled.
	 */
	public boolean isHashMode() {
		return hashMode;
	}

	/**
	 * <p>
	 * Specifies that success messages should be suppressed. Error messages will
	 * still be printed when this option is enabled.
	 * </p>
	 * <p>
	 * When encrypting many small files in certain environments, printing to
	 * standard out may consume most of the program's CPU time. This can be avoided
	 * by enabling this flag.
	 * </p>
	 * 
	 * @flag --quiet -q --suppress-success-messages -s
	 * @return <code>true</code> if success messages are being suppressed,
	 *         <code>false</code> otherwise.
	 */
	public boolean isSuppressSuccessMessages() {
		return suppressSuccessMessages;
	}

	/**
	 * Specifies the key to be used for encryption or decryption.
	 * 
	 * @flag -k --key
	 * @return The key to be used for encryption or decryption.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * <code>true</code> if the files specified when the app is launched should be
	 * decrypted rather than encrypted.
	 * 
	 * @flag --deg --decrypt -d
	 * @defaultValue <code>false</code>
	 * @return Whether encryption or decryption is being performed.
	 */
	public boolean isDecryptionMode() {
		return decryptionMode;
	}

	/**
	 * <p>
	 * Returns the buffer size used for reading files into and out of memory.
	 * Whenever a file is encrypted, its data is read into program memory (RAM) in
	 * chunks of <code>buffer-size</code> bytes at a time. Each chunk is processed
	 * (encrypted/decrypted), then written to the output, before reading the next
	 * chunk.
	 * </p>
	 * <p>
	 * If the chunk size is very small, (e.g. 1 byte), then the system will waste
	 * CPU cycles transferring bytes into memory and processing them, when it could
	 * be doing the same "in bulk." If the chunk size is far too large, (e.g.
	 * <code>20GB</code>), then the system will run out of RAM just trying to
	 * allocate the program-memory (space in RAM) to store the buffer bytes being
	 * read. (The array still gets allocated even if the file isn't that large.)
	 * </p>
	 * <p>
	 * Some implementations of underlying code (e.g. Java implementations, OS
	 * read-call implementations, hard disks and their drivers, etc.), only support
	 * reading chunks of a certain size at a time. For example, my system only
	 * allows reading chunks of 64KB at a time. If I make the buffer larger than
	 * this (e.g. 256KB), then the program will make 4 separate filesystem
	 * read-calls to fill the buffer before processing the bytes in it.
	 * </p>
	 * <p>
	 * The default value for this option is 64KB, i.e., <code>65536</code> bytes
	 * (<code>1024 * 64</code> bytes).
	 * </p>
	 * 
	 * @flag --buffer-size -bs
	 * @defaultValue 65536
	 * @return The buffer size to use.
	 */
	public int getBufferSize() {
		return bufferSize;
	}

}
