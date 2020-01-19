package sample;

import java.nio.file.NoSuchFileException;

public class NoSuchExtensionFileException extends NoSuchFileException {
    public NoSuchExtensionFileException(String file) {
        super(file);
    }
}
