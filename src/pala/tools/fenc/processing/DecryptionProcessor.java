package pala.tools.fenc.processing;

import static pala.tools.fenc.processing.EncryptionProcessor.HASH_STRING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pala.libs.generic.util.Hashing;
import pala.tools.fenc.logging.BranchLogger;
import pala.tools.fenc.logging.MessageLogger;
import pala.tools.fenc.logging.PeriodicSuccessLogger;

public class DecryptionProcessor implements DirectoryProcessor {

	private final byte[] keyHash, fileHeader;
	private final int bufferSize;
	private final BranchLogger logger;

	public DecryptionProcessor(String key, int bufferSize, MessageLogger logger) {
		this(key, bufferSize, new BranchLogger(logger));
	}

	public DecryptionProcessor(String key, int bufferSize, PeriodicSuccessLogger logger) {
		this(key, bufferSize, new BranchLogger(logger));
	}

	private DecryptionProcessor(String key, int bufferSize, BranchLogger logger) {
		keyHash = Hashing.sha256(key);
		fileHeader = Hashing.sha256(HASH_STRING + key + HASH_STRING);
		this.bufferSize = bufferSize;
		this.logger = logger;
	}

	@Override
	public void processFile(File f) {
		try {
			if (f.length() == 0)
				return;
			File temp;
			try {
				// Create a temp file as the destination for the encryption/decryption.
				temp = File.createTempFile("enc", null);
			} catch (IOException e) {
				logger.failure("TMPF",
						"Failed to create the temporary file (for intermediary processing) that file, " + f
								+ ", would get decrypted then written to. (The file was NOT decrypted.) [Err msg: "
								+ e.getLocalizedMessage() + ']');
				return;
			}
			temp.deleteOnExit();

			try {
				decryptFile(temp, f, bufferSize, fileHeader, keyHash);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
					| InvalidAlgorithmParameterException e) {
				logger.failure("EFL", "Failed to initialize the decryption algorithm while processing file: " + f
						+ ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			} catch (IOException e) {
				logger.failure("IOEX",
						"Encountered a file in-out exception while trying to read or write and decrypt the file " + f
								+ ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			} catch (FileProcessingException e) {
				logger.failure("ENEX", "Encountered an decryption failure while trying to encrypt the file " + f
						+ ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			}

			try {
				Files.copy(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.failure("TMPC", "An decrypted copy of " + f + " was written to a temporary file (" + temp
						+ ") but an issue occurred when trying to copy that temporary file back over to the source file's location. [Err msg: "
						+ e.getLocalizedMessage() + ']');
			}
			temp.delete();

		} catch (Exception e) {
			logger.failure("UNKN", "An unknown failure occurred while processing " + f
					+ ". The file may or may not have been decrypted, but should not be garbage.");
		}
	}

	private static void decryptFile(File f, File dest, int bufferSize, byte[] hdr, byte... key)
			throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, FileProcessingException {
		try (FileInputStream fis = new FileInputStream(f)) {
			byte[] header = new byte[16 + hdr.length];
			int amt = 0;
			while (amt < header.length) {
				int readcnt = fis.read(header, amt, header.length - amt);
				if (readcnt == -1)
					if (amt < header.length)
						throw new FileProcessingException("[NENC](" + f.getAbsolutePath()
								+ ") Detected a file that was not encrypted. The file does not have enough bytes (80) to contain a header. Every file encrypted by this program has an 80 byte header (64 bytes containing a unique \"encrypted-by-fenc\" hash string, and 16 containing the initialization vector needed for decryption). This file is not even 80 bytes long and so cannot have been encrypted by this program.");
					else
						break;
				amt += readcnt;
			}

			byte[] iv = Arrays.copyOfRange(header, hdr.length, 16 + hdr.length);
			for (int i = 0; i < hdr.length; i++)
				if (header[i] != hdr[i])
					throw new FileProcessingException("[NENC](" + f.getAbsolutePath() + ") Detected a file, " + f
							+ ", that was not encrypted. The file's header does not match the form of the header written to files encrypted with this program. Skipping decryption attempt of this file... ");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
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
