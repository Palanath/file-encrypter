package pala.tools.fenc.processing;

public class FileProcessingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FileProcessingException() {
	}

	public FileProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileProcessingException(String message) {
		super(message);
	}

	public FileProcessingException(Throwable cause) {
		super(cause);
	}

}
