package sample;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FileHandler {
    private RandomAccessFile file;

    public FileHandler(String path) throws FileNotFoundException {
        this.file = new RandomAccessFile(path, "r");
    }

    public String getText() throws IOException {
        FileChannel fc = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        while (fc.read(buf) != -1) {
            buf.flip();
            sb.append(Charset.defaultCharset().decode(buf));
            buf.clear();
        }
        fc.close();
        file.close();
        return sb.toString();
    }
}
