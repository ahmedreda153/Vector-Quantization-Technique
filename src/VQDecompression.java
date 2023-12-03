import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class VQDecompression {
    private int width, height;
    private int codebookSize;
    private int blockWidth, blockHeight;
    private Map<String, int[][]> codeBook;
    private String compressedStream;
    private int[][] reconstructedImage;

    // Function to read the compressed data from a file
    public static byte[] readCompressedFile(String fileName) {
        try {
            File file = new File(fileName);
            FileInputStream in = new FileInputStream(file);
            byte[] compressedBytes = new byte[in.available()];
            in.read(compressedBytes);
            in.close();
            return compressedBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Function to get the binary code of a block from the code book
    public static int[][] getBlockFromCodeBook(String binaryCode, Map<String, int[][]> codeBook) {
        return codeBook.get(binaryCode);
    }

    // Function to assign the values of the compressed data
    public void AssignValues(String fileName) throws IOException {
        byte[] compressedBytes = readCompressedFile(fileName);
        width = compressedBytes[0];
        height = compressedBytes[1];
        blockWidth = compressedBytes[2];
        blockHeight = compressedBytes[3];
        codebookSize = compressedBytes[4];
        codeBook = new java.util.HashMap<>();
        int index = 5;
        int numberOfBits = (int) Math.ceil(Math.log(codebookSize) / Math.log(2));
        for (int i = 0; i < codebookSize; ++i) {
            String binaryCode = Integer.toBinaryString(compressedBytes[index++]);
            while (binaryCode.length() < numberOfBits) {
                binaryCode = "0" + binaryCode;
            }
            int[][] block = new int[blockWidth][blockHeight];
            for (int j = 0; j < blockHeight; ++j) {
                for (int k = 0; k < blockWidth; ++k) {
                    block[j][k] = compressedBytes[index++] & 0xFF;
                }
            }
            codeBook.put(binaryCode, block);
        }
        int extraBits = compressedBytes[index++];
        compressedStream = "";
        for (int i = index; i < compressedBytes.length; ++i) {
            String binary = String.format("%8s", Integer.toBinaryString(compressedBytes[i] & 0xFF)).replace(' ', '0');
            compressedStream += binary;
        }
        compressedStream = compressedStream.substring(0, compressedStream.length() - extraBits);
    }

    // Function to write the decompressed data in a text file
    public void writeDecompressedInTextFile() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(
                "F:\\DC\\Vector-Quantization-TECHNIQUE\\decompressed.txt");
        for (int i = 0; i < reconstructedImage.length; ++i) {
            for (int j = 0; j < reconstructedImage[0].length; ++j) {
                String pixel = Integer.toString(reconstructedImage[i][j]) + " ";
                fileOutputStream.write(pixel.getBytes());
            }
            fileOutputStream.write('\n');
        }
        fileOutputStream.close();
    }

    // Function to decompress the data
    public void decompress(String fileName) throws IOException {
        AssignValues(fileName);
        reconstructedImage = new int[width][height];
        int blockIndex = 0;
        int numberOfBits = (int) Math.ceil(Math.log(codebookSize) / Math.log(2));
        for (int i = 0; i < height; i += blockHeight) {
            for (int j = 0; j < width; j += blockWidth) {
                String binaryCode = compressedStream.substring(blockIndex, blockIndex + numberOfBits);
                int[][] block = getBlockFromCodeBook(binaryCode, codeBook);
                for (int k = 0; k < blockHeight; ++k) {
                    for (int l = 0; l < blockWidth; ++l) {
                        reconstructedImage[i + k][j + l] = block[k][l];
                    }
                }
                blockIndex += numberOfBits;
            }
        }
        writeDecompressedInTextFile();
    }
}
