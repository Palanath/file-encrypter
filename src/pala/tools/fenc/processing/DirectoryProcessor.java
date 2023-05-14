package pala.tools.fenc.processing;

import java.io.File;
import java.util.Iterator;

import pala.libs.generic.JavaTools;

public interface DirectoryProcessor {
	default void handleAbnormalFileObject(File file) {
		System.err.println("[ABNF]: Failed to process the file: " + file + "; it is not a file or a directory.");
	}

	/**
	 * Processes the provided {@link File}. The {@link File} should be a file and
	 * not a directory.
	 * 
	 * @param file The file to process.
	 * @throws FileProcessingException If an error occurred with this file, but
	 *                                 processing of subsequent files can continue
	 *                                 normally.
	 */
	void processFile(File file) throws FileProcessingException;

	default void process(File file) {
		if (file.isDirectory())
			process(file.listFiles());
		else if (file.isFile())
			processFile(file);
		else
			handleAbnormalFileObject(file);
	}

	default void process(Iterator<? extends File> files) {
		if (files.hasNext())
			for (; files.hasNext(); process(files.next()))
				;
	}

	default void process(Iterable<? extends File> files) {
		process(files.iterator());
	}

	default void process(File... files) {
		process(JavaTools.iterator(files));
	}
}
