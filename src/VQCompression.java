import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

class VQCompression {
    private int width, height;
    private int[][][] originalImage;
    private int codebookSize;
    private int blockWidth, blockHeight;
    private ArrayList<int[][][]> originalImageBlocks;
    private float[][][] originalImageAverageBlock;
    private Vector<float[][][]> codeBookBlocks;
    private Map<String, int[][][]> codeBook;

    // Function to read the RGB image from a file
    public int[][][] readRGBImage(String fileName) throws IOException {
        BufferedImage img = ImageIO.read(new File(fileName));
        int width = img.getWidth();
        int height = img.getHeight();
        int[][][] imgArr = new int[width][height][3];
        Raster raster = img.getData();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 3; k++) {
                    imgArr[i][j][k] = raster.getSample(i, j, k);
                }
            }
        }
        return imgArr;
    }

    // Function to divide the RGB image into blocks
    public ArrayList<int[][][]> divideRGBImageIntoBlocks(int[][][] image, int blockWidth, int blockHeight) {
        ArrayList<int[][][]> blocks = new ArrayList<>();

        int width = image.length;
        int height = image[0].length;

        for (int i = 0; i < width; i += blockWidth) {
            for (int j = 0; j < height; j += blockHeight) {
                int currentBlockWidth = Math.min(blockWidth, width - i);
                int currentBlockHeight = Math.min(blockHeight, height - j);

                int[][][] block = new int[currentBlockWidth][currentBlockHeight][3];

                for (int k = 0; k < currentBlockWidth; ++k) {
                    for (int l = 0; l < currentBlockHeight; ++l) {
                        for (int channel = 0; channel < 3; ++channel) {
                            block[k][l][channel] = image[i + k][j + l][channel];
                        }
                    }
                }
                blocks.add(block);
            }
        }
        return blocks;
    }

    // calculate the average of blocks in a list for RGB image
    public float[][][] calculateAverageOfBlocksRGB(ArrayList<int[][][]> blocks) {
        if (blocks.isEmpty() || blocks.get(0).length == 0 || blocks.get(0)[0].length == 0) {
            return null;
        }

        int blockWidth = blocks.get(0).length;
        int blockHeight = blocks.get(0)[0].length;
        int numChannels = blocks.get(0)[0][0].length;

        float[][][] averageBlock = new float[blockHeight][blockWidth][numChannels];

        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                for (int channel = 0; channel < numChannels; ++channel) {
                    int sum = 0;
                    for (int k = 0; k < blocks.size(); ++k) {
                        if (i < blocks.get(k).length && j < blocks.get(k)[0].length) {
                            sum += blocks.get(k)[i][j][channel];
                        }
                    }
                    averageBlock[i][j][channel] = (float) sum / blocks.size();
                }
            }
        }
        return averageBlock;
    }

    // calculate the distance between two blocks for RGB image
    public int getDistanceRGB(int[][][] block1, int[][][] block2) {
        int distance = 0;

        if (block1.length != block2.length || block1[0].length != block2[0].length
                || block1[0][0].length != block2[0][0].length) {
            return -1;
        }

        int blockHeight = block1.length;
        int blockWidth = block1[0].length;
        int numChannels = block1[0][0].length;

        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                for (int channel = 0; channel < numChannels; ++channel) {
                    distance += Math.abs(block1[i][j][channel] - block2[i][j][channel]);
                }
            }
        }
        return distance;
    }

    // split a block into two blocks for RGB image
    public void splitBlockRGB(float[][][] block, int[][][] block1, int[][][] block2) {
        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                for (int channel = 0; channel < 3; ++channel) {
                    if (block[i][j][channel] % 1 == 0) {
                        block1[i][j][channel] = (int) block[i][j][channel] - 1;
                        block2[i][j][channel] = (int) block[i][j][channel] + 1;
                    } else {
                        block1[i][j][channel] = (int) Math.floor(block[i][j][channel]);
                        block2[i][j][channel] = (int) Math.ceil(block[i][j][channel]);
                    }
                }
            }
        }
    }

    // 1- split the main average block into two blocks
    // 2- compare each block from the original image with the two blocks and get two
    // lists
    // 3- calculate the average of each list
    // 4- repeat the process until reaching the number of blocks in the code book
    public void LBG() {
        codeBookBlocks = new Vector<>();
        originalImageAverageBlock = calculateAverageOfBlocksRGB(originalImageBlocks);
        codeBookBlocks.add(originalImageAverageBlock);

        while (codeBookBlocks.size() < codebookSize) {
            Vector<float[][][]> newCodeBookBlocks = new Vector<>();
            ArrayList<int[][][]> tempOriginalImageBlocks = originalImageBlocks;

            for (int i = 0; i < codeBookBlocks.size(); ++i) {
                int[][][] block1 = new int[blockWidth][blockHeight][3];
                int[][][] block2 = new int[blockWidth][blockHeight][3];
                splitBlockRGB(codeBookBlocks.get(i), block1, block2);

                ArrayList<int[][][]> list1 = new ArrayList<>();
                ArrayList<int[][][]> list2 = new ArrayList<>();

                for (int j = 0; j < tempOriginalImageBlocks.size(); ++j) {
                    int distance1 = getDistanceRGB(tempOriginalImageBlocks.get(j), block1);
                    int distance2 = getDistanceRGB(tempOriginalImageBlocks.get(j), block2);

                    if (distance1 <= distance2) {
                        list1.add(tempOriginalImageBlocks.get(j));
                    } else {
                        list2.add(tempOriginalImageBlocks.get(j));
                    }
                }

                if (i + 1 < codeBookBlocks.size()) {
                    int[][][] block3 = new int[blockWidth][blockHeight][3];
                    int[][][] block4 = new int[blockWidth][blockHeight][3];
                    splitBlockRGB(codeBookBlocks.get(i + 1), block3, block4);

                    ArrayList<int[][][]> list3 = new ArrayList<>();

                    for (int j = 0; j < list2.size();) {
                        int distance3 = getDistanceRGB(list2.get(j), block2);
                        int distance4 = getDistanceRGB(list2.get(j), block3);

                        if (distance3 > distance4) {
                            list3.add(list2.get(j));
                            list2.remove(j);
                        } else {
                            ++j;
                        }
                    }

                    tempOriginalImageBlocks = list3;
                }

                float[][][] averageBlock1 = calculateAverageOfBlocksRGB(list1);
                float[][][] averageBlock2 = calculateAverageOfBlocksRGB(list2);
                newCodeBookBlocks.add(averageBlock1);
                newCodeBookBlocks.add(averageBlock2);
            }

            codeBookBlocks = newCodeBookBlocks;
        }
    }

    // convert float array to int array for RGB image
    public int[][][] convertFloatArrayToIntArrayRGB(float[][][] doubleArray) {
        int[][][] intArray = new int[blockWidth][blockHeight][3];
        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                for (int channel = 0; channel < 3; ++channel) {
                    intArray[i][j][channel] = (int) doubleArray[i][j][channel];
                }
            }
        }
        return intArray;
    }

    // generate binary code (label) for each block in the code book for RGB image
    public Map<String, int[][][]> generateCodeBookRGB(int codebookSize) {
        Map<String, int[][][]> codeBook = new java.util.HashMap<>();
        int numberOfBits = (int) Math.ceil(Math.log(codebookSize) / Math.log(2));
        for (int i = 0; i < codebookSize; ++i) {
            String binary = Integer.toBinaryString(i);
            while (binary.length() < numberOfBits) {
                binary = "0" + binary;
            }
            codeBook.put(binary, convertFloatArrayToIntArrayRGB(codeBookBlocks.get(i)));
        }
        return codeBook;
    }

    // get the binary code (label) of a block for RGB image
    public String getBinaryCodeRGB(int[][][] block) {
        int minDistance = Integer.MAX_VALUE;
        String binaryCode = "";

        for (Map.Entry<String, int[][][]> entry : codeBook.entrySet()) {
            int distance = getDistanceRGB(block, entry.getValue());

            if (distance < minDistance) {
                minDistance = distance;
                binaryCode = entry.getKey();
            }
        }
        return binaryCode;
    }

    // write the all needed data in a file for RGB image
    public void writeDataInFileRGB() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(
                "F:\\Vector-Quantization-TECHNIQUE_RGB\\compressed.txt");

        // store width in 4 bytes
        String binaryWidth = Integer.toBinaryString(width);
        while (binaryWidth.length() < 32) {
            binaryWidth = "0" + binaryWidth;
        }
        for (int i = 0; i < 32; i += 8) {
            fileOutputStream.write(Integer.parseInt(binaryWidth.substring(i, i + 8), 2));
        }

        // store height in 4 bytes
        String binaryHeight = Integer.toBinaryString(height);
        while (binaryHeight.length() < 32) {
            binaryHeight = "0" + binaryHeight;
        }
        for (int i = 0; i < 32; i += 8) {
            fileOutputStream.write(Integer.parseInt(binaryHeight.substring(i, i + 8), 2));
        }

        fileOutputStream.write(blockWidth);
        fileOutputStream.write(blockHeight);
        fileOutputStream.write(codebookSize);

        // write the code book (label, block) for RGB image
        for (Map.Entry<String, int[][][]> entry : codeBook.entrySet()) {
            fileOutputStream.write(Integer.parseInt(entry.getKey(), 2));
            for (int i = 0; i < blockHeight; ++i) {
                for (int j = 0; j < blockWidth; ++j) {
                    for (int channel = 0; channel < 3; ++channel) {
                        fileOutputStream.write(entry.getValue()[i][j][channel]);
                    }
                }
            }
        }

        // write the binary code of each block in the original image in a string for RGB
        // image
        String compressedStream = "";
        for (int i = 0; i < originalImageBlocks.size(); ++i) {
            String binaryCode = getBinaryCodeRGB(originalImageBlocks.get(i));
            compressedStream += binaryCode;
        }

        // calculate the number of extra bits to make the length of the compressed
        // stream divisible by 8
        int extraBits;
        if (compressedStream.length() % 8 == 0) {
            extraBits = 0;
        } else {
            extraBits = 8 - (compressedStream.length() % 8);
        }

        // write the number of extra bits in the file
        fileOutputStream.write(extraBits);

        // add the extra bits to the compressed stream
        for (int i = 0; i < extraBits; ++i) {
            compressedStream += "0";
        }

        // write the compressed stream in the file
        for (int i = 0; i < compressedStream.length(); i += 8) {
            fileOutputStream.write(Integer.parseInt(compressedStream.substring(i, i + 8), 2));
        }

        fileOutputStream.close();
    }

    // Function to compress the original data for RGB image
    public void compressRGB(String fileName) throws IOException {
        this.codebookSize = 32;
        this.originalImage = readRGBImage(fileName);
        this.width = originalImage[0].length;
        this.height = originalImage.length;
        this.blockWidth = 2;
        this.blockHeight = 2;
        this.originalImageBlocks = divideRGBImageIntoBlocks(originalImage, blockWidth, blockHeight);
        LBG();
        this.codeBook = generateCodeBookRGB(codebookSize);
        writeDataInFileRGB();
    }

}