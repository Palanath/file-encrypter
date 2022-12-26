package pala.tools.fenc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pala.libs.generic.JavaTools;
import pala.libs.generic.parsers.cli.CLIParams;
import pala.libs.generic.util.Hashing;

public class FileEncrypter {
	/**
	 * <p>
	 * Encrypts files and directories specified by arguments. Each file is processed
	 * separately, even if a directory is specified and files are contained in that
	 * directory.
	 * </p>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Example program invocation (for my own use, so that I can write the readme):
		// java -jar fenc.jar -k="Some key" --dec -f F:/some/folder/on/f/drive
		// /some/file/on/current/drive.txt some/relative/folder/
		CLIParams flags = new CLIParams(args);
		Options options = new Options(flags);

		List<String> specifiedFilePaths = flags.getUnnamed();
		List<File> files = JavaTools.addAll(specifiedFilePaths, File::new, new ArrayList<>(specifiedFilePaths.size()));
		for (File f : files)
			try {
				process(f, options);
			} catch (Exception e) {
				System.err.println("Exception processing: " + f);
				e.printStackTrace();
			}
	}

	public static void process(File f, Options options)
			throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (f.isFile())
			processFile(f, options);
		else if (f.isDirectory())
			try {
				for (File i : f.listFiles())
					process(i, options);
			} catch (Exception e) {
				System.err.println("Failed to iterate over files in " + f);
				e.printStackTrace();
			}
	}

	public static void processFile(File f, Options options)
			throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
		// Expects f.isFile() to return true.
		byte[] hash = Hashing.sha256(options.getKey());

		// Create a temp file as the destination for the encryption/decryption.
		File temp = File.createTempFile("enc", null);
		temp.deleteOnExit();

		try {
			if (options.isDecryptionMode())
				decryptFile(f, temp, options.getBufferSize(), hash);
			else
				encryptFile(f, temp, options.getBufferSize(), hash);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		Files.copy(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		temp.delete();
	}

	/**
	 * Reads bytes from the specified {@link File}, <code>f</code>, encrypts them
	 * using the specified key and AES, then outputs the result into the destination
	 * file. The encryption result contains the initialization vector (16 bytes)
	 * followed immediately by the ciphertext.
	 * 
	 * @param f    The source file to read bytes from.
	 * @param dest The destination file to output the encryption result to.
	 * @param key  The key used for the encryption.
	 * @throws NoSuchAlgorithmException           If the underlying {@link Cipher}
	 *                                            object instantiation throws a
	 *                                            {@link NoSuchAlgorithmException}.
	 * @throws NoSuchPaddingException             If the underlying {@link Cipher}
	 *                                            object instantiation throws a
	 *                                            {@link NoSuchPaddingException}.
	 * @throws InvalidKeyException                If the specified key is not valid
	 *                                            for AES.
	 * @throws InvalidAlgorithmParameterException If an error in the underlying
	 *                                            cryptography implementation
	 *                                            occurs, causing it to reject the
	 *                                            initialization vector provided, or
	 *                                            otherwise reject the
	 *                                            {@link Cipher} object
	 *                                            initialization parameterization.
	 * @throws IOException                        If an {@link IOException} occurs
	 *                                            while reading/writing to the
	 *                                            filesystem.
	 */
	public static void encryptFile(File f, File dest, int bufferSize, byte... key) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {

		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		SecureRandom ran = new SecureRandom();
		byte[] iv = new byte[16];
		ran.nextBytes(iv);
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

		try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
			fileOutputStream.write(iv);
			try (CipherOutputStream cos = new CipherOutputStream(fileOutputStream, cipher)) {
				try (FileInputStream fis = new FileInputStream(f)) {
					int amt;
					byte buff[] = new byte[bufferSize];
					while ((amt = fis.read(buff)) != -1)
						cos.write(buff, 0, amt);
				}
			}
		}

	}

	public static void decryptFile(File f, File dest, int bufferSize, byte... key) throws IOException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		try (FileInputStream fis = new FileInputStream(f)) {
			byte[] iv = new byte[16];
			int amt = 0;
			while (amt < iv.length)
				// If amount is decreased, then fis.read returned a negative number (i.e. -1),
				// meaning the end of the stream was reached (before amt hit 16), so less than
				// 16 bytes were read. This means that the file (f) did not contain an
				// initialization vector.
				if (amt < (amt += fis.read(iv, amt, iv.length - amt)))
					if (amt < iv.length)
						throw new IllegalArgumentException(
								"Specified source file does not have enough bytes to contain an initialization vector. This file is not ciphertext created by this program.");
					else
						break;

			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
				byte[] buff = new byte[bufferSize];
				try (FileOutputStream fos = new FileOutputStream(dest)) {
					while ((amt = cis.read(buff)) != -1)
						fos.write(buff, 0, amt);
				}
			}
		}
	}

}
