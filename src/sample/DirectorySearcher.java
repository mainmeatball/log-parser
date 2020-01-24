package sample;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import sample.exceptions.NoSuchDirectoryException;
import sample.exceptions.NoSuchExtensionFileException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectorySearcher extends Task<TreeItem<String>> {
    private String path;
    private String extension;
    private String searchInput;

    public DirectorySearcher(String path, String extension, String searchInput) {
        this.path = path;
        this.extension = extension;
        this.searchInput = searchInput;
    }

    @Override
    public TreeItem<String> call() throws IOException {
        return search();
    }

    private TreeItem<String> search() throws IOException {
        Stream<Path> walk;
        try {
            walk = Files.walk(Paths.get(path));
        } catch (NoSuchFileException e) {
            throw new NoSuchDirectoryException(e.getFile());
        }
        List<String> result = walk.map(Path::toString)
                .filter(f -> f.endsWith("." + extension))
                .map(s -> s.substring(path.length() + 1))
                .collect(Collectors.toList());

        walk.close();
        if (result.isEmpty()) {
            throw new NoSuchExtensionFileException(path);
        }
        TreeItem<String> root = createDirectoryTreeFrom(result, searchInput);
        if (root.getChildren().isEmpty()) {
            throw new NoSuchFileException(path);
        }
        return root;
    }

        private TreeItem<String> createDirectoryTreeFrom(Collection<String> collection, String searchInput) throws IOException {
            // Loop through files list
            TreeItem<String> root = new TreeItem<>(path.substring(path.lastIndexOf(File.separator) + 1));
            root.setExpanded(true);
            for (String p : collection) {
                RandomAccessFile file = new RandomAccessFile(path + File.separator + p, "r");
                FileChannel fc = file.getChannel();
                ByteBuffer buf = ByteBuffer.allocate(4096);
                while (fc.read(buf) != -1) {
                    buf.flip();
                    if (KMPMatch.indexOf(buf.array(), searchInput.getBytes()) != -1) {
                        String[] filePathArray = p.split(Pattern.quote(File.separator));
                        TreeItem<String> tempRoot = root;
                        for (String fileName : filePathArray) {
                            // Сheck if there is a file with the same name in the folder recursive way (change)
                            TreeItem<String> findNode = findItemIn(tempRoot, fileName);
                            if (findNode != null) {
                                tempRoot = findNode;
                            } else {
                                TreeItem<String> node = new TreeItem<>(fileName);
                                tempRoot.getChildren().add(node);
                                tempRoot.setExpanded(true);
                                tempRoot = node;
                            }
                        }
                        break;
                    }
                    buf.clear();
                }
                fc.close();
                file.close();
            }
            return root;
        }

    public TreeItem<String> findItemIn(TreeItem<String> container, String predicate) {
        if (container != null && container.getValue().equals(predicate)) {
            return container;
        }
        if (container == null) {
            return null;
        }
        for (TreeItem<String> child : container.getChildren()) {
            if (child.getValue().equals(predicate)) {
                return container;
            }
        }
        return null;
    }

}
