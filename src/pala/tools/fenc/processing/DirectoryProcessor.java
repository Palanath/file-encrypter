package pala.tools.fenc.processing;

import java.io.File;
import java.util.Iterator;

import pala.libs.generic.JavaTools;
import pala.tools.fenc.Options;

public abstract class DirectoryProcessor {

	private final Options options;

	public DirectoryProcessor(Options options) {
		this.options = options;
	}

	protected Options getOptions() {
		return options;
	}

	/**
	 * <p>
	 * Used by subclasses to print a message on success. The prefix is prepended to
	 * the message in the format:
	 * </p>
	 * 
	 * <pre>
	 * <code>[prefix]: message</code>
	 * </pre>
	 * 
	 * <p>
	 * and the result is sent to standard out unless suppressed or configured
	 * otherwise by the caller.
	 * </p>
	 * 
	 * @param prefix  The prefix for the message, used by log-parsing utils to
	 *                easily identify the message.
	 * @param message The message itself.
	 */
	protected final void success(String prefix, String message) {

	}

	/**
	 * <p>
	 * Used by subclasses to print a message on failure. The prefix is prepended to
	 * the message in the format:
	 * </p>
	 * 
	 * <pre>
	 * <code>[prefix]: message</code>
	 * </pre>
	 * 
	 * <p>
	 * and the result is sent to standard error, usually immediately even if
	 * suppression is enabled.
	 * </p>
	 * 
	 * @param prefix  The prefix for the message, used by log-parsing utils to
	 *                easily identify the message.
	 * @param message The message itself.
	 */
	protected final void failure(String prefix, String message) {

	}

	protected void handleAbnormalFileObject(File file) {
		System.err.println("Failed to process the file: " + file + "; it is not a file or a directory.");
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
