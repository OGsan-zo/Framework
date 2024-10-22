package other; 

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUpload {
    private String fileName;
    private String filePath;
    private byte[] fileData;

    public FileUpload(String fileName, String filePath, byte[] fileData) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileData = fileData;
    }

    // Getters et setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    // Méthode pour enregistrer le fichier sur le disque
    public void saveFile() throws IOException {
        Path path = Paths.get(filePath, fileName);
        Files.write(path, fileData);
    }
}
