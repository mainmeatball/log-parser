package sample;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FileHandler {
    String path;

    public FileHandler(String path) {
        this.path = path;
    }

    public String getText() throws IOException {
        RandomAccessFile file = new RandomAccessFile(path, "r");
        FileChannel fc = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        while (fc.read(buf) != -1) {
            buf.flip();
            sb.append(Charset.defaultCharset().decode(buf));
            buf.clear();
        }
        fc.close();
        return sb.toString();
    }
}
