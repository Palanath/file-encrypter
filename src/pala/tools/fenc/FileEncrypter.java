package pala.tools.fenc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import pala.libs.generic.JavaTools;
import pala.libs.generic.parsers.cli.CLIParams;

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

	public static void process(File f, Options options) throws IOException {
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

	public static void processFile(File f, Options options) throws IOException {
		// Expects f.isFile() to return true.
		if (!options.isFastMode()) {
			// Copy to temp directory first and attempt the encryption/decryption, so that
			// if something goes wrong in the middle, the original file won't be damaged.
			File temp = File.createTempFile("enc", null);
			temp.deleteOnExit();
			Files.copy(f.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
			if (options.isDecryptionMode())
				decryptFileInPlace(f, options.getKey());
			else
				encryptFileInPlace(f, options.getKey());
			Files.copy(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			temp.delete();
		} else if (options.isDecryptionMode())
			decryptFileInPlace(f, options.getKey());
		else
			encryptFileInPlace(f, options.getKey());
	}

	public static void encryptFileInPlace(File f, String key) {

	}

	public static void decryptFileInPlace(File f, String key) {

	}

}
