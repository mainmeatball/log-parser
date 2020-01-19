package sample;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
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
    public TreeItem<String> call() throws IOException, NoSuchExtensionFileException {
        return search();
    }

    private TreeItem<String> search() throws IOException, NoSuchExtensionFileException {
        Stream<Path> walk = Files.walk(Paths.get(path));
//         find all files which end with extension and put them into list
        List<String> result = walk.map(Path::toString)
                .filter(f -> f.endsWith("." + extension))
                .map(s -> s.substring(path.length() + 1))
                .collect(Collectors.toList());

        walk.close();
        if (result.isEmpty()) {
            throw new NoSuchExtensionFileException();
        }
        TreeItem<String> root = createDirectoryTreeFrom(result, searchInput);
        if (root.getChildren().isEmpty()) {
            throw new NoSuchFileException(path);
        }
        return root;
    }

        private TreeItem<String> createDirectoryTreeFrom(Collection<String> collection, String searchInput) throws IOException {

            // loop through files list
            TreeItem<String> root = new TreeItem<>(path.substring(path.lastIndexOf("/")));
            root.setExpanded(true);
            for (String p : collection) {

                // read a file using buffer (works with big files)
                FileReader file = new FileReader(path + "/" + p);
                BufferedReader reader = new BufferedReader(file);
                String line;
                while((line = reader.readLine()) != null) {
                    if((line.contains(searchInput))) {
                        String[] filePathArray = p.split("/");
                        TreeItem<String> tempRoot = root;
                        for (String fileName : filePathArray) {

                            // check if there is a file with the same name in the folder
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
                }
                file.close();
                reader.close();
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
            TreeItem<String> s = findItemIn(child, predicate);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

}
