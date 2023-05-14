package pala.tools.fenc.processing;

import java.io.File;
import java.util.Iterator;

import pala.libs.generic.JavaTools;
import pala.tools.fenc.Options;
import pala.tools.fenc.logging.StatusLogger;

public abstract class DirectoryProcessor {

	private final Options options;
	private final StatusLogger logger;

	public DirectoryProcessor(Options options) {
		this(options, StatusLogger.simpleLogger());
	}

	public DirectoryProcessor(Options options, StatusLogger logger) {
		this.options = options;
		this.logger = logger;
	}

	protected final Options getOptions() {
		return options;
	}

	protected final StatusLogger getLogger() {
		return logger;
	}

	protected void handleAbnormalFileObject(File file) {
		logger.failure("ABNF", "Failed to process the file: " + file + "; it is not a file or a directory.");
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
	protected abstract void processFile(File file) throws FileProcessingException;

	public final void process(File file) {
		if (file.isDirectory())
			process(file.listFiles());
		else if (file.isFile())
			processFile(file);
		else
			handleAbnormalFileObject(file);
	}

	public final void process(Iterator<? extends File> files) {
		if (files.hasNext())
			for (; files.hasNext(); process(files.next()))
				;
	}

	public final void process(Iterable<? extends File> files) {
		process(files.iterator());
	}

	public final void process(File... files) {
		process(JavaTools.iterator(files));
	}
}
