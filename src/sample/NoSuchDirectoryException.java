package sample;

import java.nio.file.NoSuchFileException;

public class NoSuchDirectoryException extends NoSuchFileException {
    public NoSuchDirectoryException(String file) {
        super(file);
    }
}
