import java.io.IOException;

public class VectorQuantizationLBG {
    private VQCompression vqCompression = new VQCompression();
    private VQDecompression vqDecompression = new VQDecompression();

    public void compress(String fileName) throws IOException {
        vqCompression.compress(fileName);
    }

    public void decompress(String fileName) throws IOException {
        vqDecompression.decompress(fileName);
    }
    
    public static void main(String[] args) throws IOException {
        new VQApp();
    }
}