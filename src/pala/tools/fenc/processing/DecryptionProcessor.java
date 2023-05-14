package pala.tools.fenc.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pala.tools.fenc.logging.BranchLogger;
import pala.tools.fenc.logging.MessageLogger;
import pala.tools.fenc.logging.PeriodicSuccessLogger;

public class DecryptionProcessor extends CipherProcessor {

	public DecryptionProcessor(String key, int bufferSize, MessageLogger logger) {
		this(key, bufferSize, new BranchLogger(logger));
	}

	public DecryptionProcessor(String key, int bufferSize, PeriodicSuccessLogger logger) {
		this(key, bufferSize, new BranchLogger(logger));
	}

	private DecryptionProcessor(String key, int bufferSize, BranchLogger logger) {
		super("decrypt", DecryptionProcessor::decryptFile, key, bufferSize, logger);
	}

	public static void decryptFile(File f, File dest, int bufferSize, byte[] hdr, byte... key)
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
