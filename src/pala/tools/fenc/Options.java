package pala.tools.fenc;

import java.security.SecureRandom;

import pala.libs.generic.parsers.cli.CLIParams;

public class Options {

	public enum Mode {
		/**
		 * <p>
		 * Specifies that encryption mode should be used. This is the case by default
		 * when none of {@link #DECRYPT}, {@link #HASH}, and {@link #KEYGEN} is the
		 * selected mode.
		 * </p>
		 * <p>
		 * This mode expects a {@link Options#getKey() key} to be specified. This mode
		 * responds to {@link Options#isSuppressSuccessMessages()} and
		 * {@link Options#getBufferSize()}.
		 * </p>
		 * <p>
		 * This mode encrypts all the specified files using the provided key. The
		 * encrypted files are prepended with a 256-bit hash of the text "Encrypted by
		 * FEnc." followed immediately by the provided key, followed again by "Encrypted
		 * by FEnc.". Note that this hash string is <i>not</i> salted.
		 * </p>
		 * <p>
		 * When encrypting a file, the program checks if the file about to be encrypted
		 * contains the hash-header it expects. If the header is present, then that file
		 * has already been encrypted with the provided key, so the program prints a
		 * message and does not modify the file.
		 * </p>
		 */
		ENCRYPT, DECRYPT, HASH,
		/**
		 * <p>
		 * This mode is used solely to generate secure keys. It utilizes the specified
		 * {@link Options#getKeygenSize() keygen size}, the and, if provided, the
		 * provided {@link Options#getKey() key} as a source for the random generator.
		 * </p>
		 * <p>
		 * This mode creates a new {@link SecureRandom} and generates the specified
		 * number of random characters with it. The random character set can be
		 * </p>
		 */
		KEYGEN
	}

	private final String key;
	private final boolean suppressSuccessMessages;
	private final int bufferSize;
	private Mode mode;
	private final int keygenSize;

	private void setMode(Mode mode) {
		if (mode == null)
			throw new RuntimeException("Two separate modes specified: " + this.mode + ", and " + mode);
		this.mode = mode;
	}

	public Options(CLIParams params) {
		key = params.readString((String) null, "-k", "--key");
		if (params.checkFlag(false, "--dec", "--decrypt", "-d"))
			mode = Mode.DECRYPT;
		if (params.checkFlag(false, "-h", "--hash"))
			setMode(Mode.DECRYPT);
		if (params.checkFlag(false, "-kg", "--keygen"))
			setMode(Mode.KEYGEN);
		if (mode == null)
			mode = Mode.ENCRYPT;
		bufferSize = params.readInt(65536, "--buffer-size", "-bs");
		suppressSuccessMessages = params.checkFlag(false, "--quiet", "-q", "--suppress-success-messages", "-s");
		keygenSize = params.readInt(10, "-ks", "--keygen-size", "--key-size");
	}

	public int getKeygenSize() {
		return keygenSize;
	}

	/**
	 * Determines whether the program is in hash mode. In hash mode, the program
	 * only hashes all the files it encounters; it does not encrypt or decrypt
	 * anything. This option cannot be used with {@link #isDecryptionMode()
	 * decryption mode}. When this option is specified, a {@link #getKey() key}
	 * should not be specified. This option considers the {@link #getBufferSize()
	 * bufferSize} when reading in data from a file to hash. All other options are
	 * ignored.
	 * 
	 * @return Whether hash mode is enabled.
	 */
	public boolean isHashMode() {
		return mode == Mode.HASH;
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
	 * @return Whether decryption is being performed.
	 */
	public boolean isDecryptionMode() {
		return mode == Mode.DECRYPT;
	}

	/**
	 * <code>true</code> if the files specified when the app is launched should be
	 * encrypted. This mode is enabled by default if {@link #isDecryptionMode()},
	 * {@link #isHashMode()}, and {@link #isKeygenMode()} are not used.
	 * 
	 * @return Whether encryption is being performed.
	 */
	public boolean isEncryptionMode() {
		return mode == Mode.ENCRYPT;
	}

	/**
	 * <code>true</code> if the app is to generate a key rather than encrypt,
	 * decrypt, or hash.
	 * 
	 * @flag --keygen -k
	 * @return <code>true</code> if {@link Mode#KEYGEN} is the selected mode.
	 */
	public boolean isKeygenMode() {
		return mode == Mode.KEYGEN;
	}

	public Mode getMode() {
		return mode;
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
