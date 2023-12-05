import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

public class VQDecompression {
    private int width, height;
    private int codebookSize;
    private int blockWidth, blockHeight;
    private Map<String, int[][][]> codeBook;
    private String compressedStream;
    private int[][][] reconstructedImage;

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
    public static int[][][] getBlockFromCodeBook(String binaryCode, Map<String, int[][][]> codeBook) {
        return codeBook.get(binaryCode);
    }

    // Function to assign the values of the compressed data
    public void AssignValues(String fileName) throws IOException {
        byte[] compressedBytes = readCompressedFile(fileName);
        // read first 4 bytes as width
        for (int i = 0; i < 4; ++i) {
            width = (width << 8) + (compressedBytes[i] & 0xFF);
        }

        // read next 4 bytes as height
        for (int i = 4; i < 8; ++i) {
            height = (height << 8) + (compressedBytes[i] & 0xFF);
        }

        System.out.println("Width: " + width + ", Height: " + height);

        blockWidth = compressedBytes[8];
        blockHeight = compressedBytes[9];
        codebookSize = compressedBytes[10];
        codeBook = new java.util.HashMap<>();
        int index = 11;
        int numberOfBits = (int) Math.ceil(Math.log(codebookSize) / Math.log(2));
        for (int i = 0; i < codebookSize; ++i) {
            String binaryCode = Integer.toBinaryString(compressedBytes[index++]);
            while (binaryCode.length() < numberOfBits) {
                binaryCode = "0" + binaryCode;
            }
            int[][][] block = new int[blockWidth][blockHeight][3]; // Modified for RGB
            for (int j = 0; j < blockWidth; ++j) {
                for (int k = 0; k < blockHeight; ++k) {
                    for (int channel = 0; channel < 3; ++channel) {
                        block[j][k][channel] = compressedBytes[index++];
                    }
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

    // Function to generate the decompressed RGB image
    public void generateDecompressedImageRGB(int[][][] image) throws IOException {
        flipImageHorizontallyRGB(image);
        rotate270RGB(image);
        String newPath = "F:\\Vector-Quantization-TECHNIQUE_RGB\\decompressed_rgb.png";
        BufferedImage bufferedImage = new BufferedImage(image.length, image[0].length, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = bufferedImage.getRaster();

        int[] flatArray = new int[image.length * image[0].length * 3];
        int flatIndex = 0;
        for (int i = 0; i < image.length; ++i) {
            for (int j = 0; j < image[0].length; ++j) {
                for (int channel = 0; channel < 3; ++channel) {
                    flatArray[flatIndex++] = image[i][j][channel];
                }
            }
        }

        raster.setPixels(0, 0, image.length, image[0].length, flatArray);
        File file = new File(newPath);
        file.createNewFile();
        ImageIO.write(bufferedImage, "png", file);
    }

    // Function to flip the RGB image horizontally
    public void flipImageHorizontallyRGB(int[][][] image) {
        for (int i = 0; i < image.length; ++i) {
            for (int j = 0; j < image[0].length / 2; ++j) {
                int[] temp = image[i][j];
                image[i][j] = image[i][image[0].length - j - 1];
                image[i][image[0].length - j - 1] = temp;
            }
        }
    }

    // Function to rotate the RGB image 270 degrees
    public static void rotate270RGB(int[][][] original) {
        int[][][] rotated = new int[original[0].length][original.length][3]; // Modified for RGB

        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                int newI = original[0].length - 1 - j;
                int newJ = i;
                rotated[newI][newJ] = original[i][j];
            }
        }

        for (int i = 0; i < original[0].length; i++) {
            System.arraycopy(rotated[i], 0, original[i], 0, original.length);
        }
    }

    // Function to decompress the data for RGB image
    public void decompressRGB(String fileName) throws IOException {
        AssignValues(fileName);

        reconstructedImage = new int[width][height][3];
        int blockIndex = 0;
        int numberOfBits = (int) Math.ceil(Math.log(codebookSize) / Math.log(2));

        for (int i = 0; i < width; i += blockWidth) {
            for (int j = 0; j < height; j += blockHeight) {
                int currentBlockWidth = Math.min(blockWidth, width - i);
                int currentBlockHeight = Math.min(blockHeight, height - j);

                String binaryCode = compressedStream.substring(blockIndex * numberOfBits,
                        (blockIndex + 1) * numberOfBits);

                int[][][] block = getBlockFromCodeBook(binaryCode, codeBook);

                for (int k = 0; k < currentBlockWidth; ++k) {
                    for (int l = 0; l < currentBlockHeight; ++l) {
                        for (int channel = 0; channel < 3; ++channel) {
                            reconstructedImage[i + k][j + l][channel] = block[k][l][channel];
                        }
                    }
                }
                
                ++blockIndex;
            }
        }

        generateDecompressedImageRGB(reconstructedImage);
    }

}
