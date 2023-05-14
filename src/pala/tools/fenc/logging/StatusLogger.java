package pala.tools.fenc.logging;

public interface StatusLogger {
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
	 * and the result is usually sent to standard out unless suppressed or
	 * configured otherwise by the caller.
	 * </p>
	 * 
	 * @param prefix  The prefix for the message, used by log-parsing utils to
	 *                easily identify the message.
	 * @param message The message itself.
	 */
	void success(String prefix, String message);

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
	 * and the result is usually sent to standard error, usually immediately even if
	 * suppression is enabled.
	 * </p>
	 * 
	 * @param prefix  The prefix for the message, used by log-parsing utils to
	 *                easily identify the message.
	 * @param message The message itself.
	 */
	void failure(String prefix, String message);

	class SimpleLogger implements StatusLogger {
		@Override
		public void success(String prefix, String message) {
			System.out.println('[' + prefix + "]: " + message);
		}

		@Override
		public void failure(String prefix, String message) {
			System.err.println('[' + prefix + "]: " + message);
		}
	}

	static SimpleLogger simpleLogger() {
		return new SimpleLogger();
	}
}
