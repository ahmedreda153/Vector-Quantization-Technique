import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

class VQCompression {
    private int width, height;
    private int[][] originalImage;
    private int codebookSize;
    private int blockWidth, blockHeight;
    private ArrayList<int[][]> originalImageBlocks;
    private float[][] originalImageAverageBlock;
    private Vector<float[][]> codeBookBlocks;
    private Map<String, int[][]> codeBook;

    // Function to read and know the width of the 2D array from a text file
    public int getWidth(String fileName) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line = br.readLine();
            String[] values = line.split(" ");
            int width = 0;

            for (String value : values) {
                if (!value.isEmpty()) {
                    width++;
                }
            }

            return width;
        }
    }

    // Function to read and know the height of the 2D array from a text file
    public int getHeight(String fileName) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            int height = 0;
            while (br.readLine() != null) {
                height++;
            }
            return height;
        }
    }

    // Function to read the 2d array from a text file
    public int[][] read2DArray(String fileName) throws IOException {
        int width = getWidth(fileName);
        int height = getHeight(fileName);
        int[][] array = new int[height][width];

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for (int i = 0; i < height; i++) {
                String[] values = br.readLine().split(" ");
                int j = 0;

                for (String value : values) {
                    if (!value.isEmpty()) {
                        array[i][j++] = Integer.parseInt(value);
                    }
                }
            }
        }

        return array;
    }

    // divide the image into blocks of size 2x2 (blockWidth x blockHeight)
    public ArrayList<int[][]> divideImageIntoBlocks(int[][] image, int blockWidth, int blockHeight) {
        ArrayList<int[][]> blocks = new ArrayList<>();
        for (int i = 0; i < height; i += blockHeight) {
            for (int j = 0; j < width; j += blockWidth) {
                int[][] block = new int[blockWidth][blockHeight];
                for (int k = 0; k < blockHeight; ++k) {
                    for (int l = 0; l < blockWidth; ++l) {
                        block[k][l] = image[i + k][j + l];
                    }
                }
                blocks.add(block);
            }
        }
        return blocks;
    }

    // calculate the average of blocks in a list
    public float[][] calculateAverageOfBlocks(ArrayList<int[][]> blocks) {
        float[][] averageBlock = new float[blockWidth][blockHeight];
        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                int sum = 0;
                for (int k = 0; k < blocks.size(); ++k) {
                    sum += blocks.get(k)[i][j];
                }
                averageBlock[i][j] = (float) sum / blocks.size();
            }
        }
        return averageBlock;
    }

    // calculate the distance between two blocks
    public int getDistance(int[][] block1, int[][] block2) {
        int distance = 0;
        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                distance += Math.abs(block1[i][j] - block2[i][j]);
            }
        }
        return distance;
    }

    // split a block into two blocks
    public void splitBlock(float[][] block, int[][] block1, int[][] block2) {
        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                if (block[i][j] % 1 == 0) {
                    block1[i][j] = (int) block[i][j] - 1;
                    block2[i][j] = (int) block[i][j] + 1;
                } else {
                    block1[i][j] = (int) Math.floor(block[i][j]);
                    block2[i][j] = (int) Math.ceil(block[i][j]);
                }
            }
        }
    }

    // 1- split the main average block into two blocks
    // 2- compare each block from the original image with the two blocks and get two lists
    // 3- calculate the average of each list
    // 4- repeat the process until reaching the number of blocks in the code book
    public void LBG() {
        codeBookBlocks = new Vector<>();
        originalImageAverageBlock = calculateAverageOfBlocks(originalImageBlocks);
        codeBookBlocks.add(originalImageAverageBlock);
        while (codeBookBlocks.size() < codebookSize) {
            Vector<float[][]> newCodeBookBlocks = new Vector<>();
            ArrayList<int[][]> tempOriginalImageBlocks = originalImageBlocks;
            for (int i = 0; i < codeBookBlocks.size(); ++i) {
                int[][] block1 = new int[blockWidth][blockHeight];
                int[][] block2 = new int[blockWidth][blockHeight];
                splitBlock(codeBookBlocks.get(i), block1, block2);
                ArrayList<int[][]> list1 = new ArrayList<>();
                ArrayList<int[][]> list2 = new ArrayList<>();
                for (int j = 0; j < tempOriginalImageBlocks.size(); ++j) {
                    int distance1 = getDistance(tempOriginalImageBlocks.get(j), block1);
                    int distance2 = getDistance(tempOriginalImageBlocks.get(j), block2);
                    if (distance1 <= distance2) {
                        list1.add(tempOriginalImageBlocks.get(j));
                    } else {
                        list2.add(tempOriginalImageBlocks.get(j));
                    }
                }
                if (i + 1 < codeBookBlocks.size()) {
                    int[][] block3 = new int[blockWidth][blockHeight];
                    int[][] block4 = new int[blockWidth][blockHeight];
                    splitBlock(codeBookBlocks.get(i + 1), block3, block4);
                    ArrayList<int[][]> list3 = new ArrayList<>();
                    for (int j = 0; j < list2.size();) {
                        int distance3 = getDistance(list2.get(j), block2);
                        int distance4 = getDistance(list2.get(j), block3);
                        if (distance3 > distance4) {
                            list3.add(list2.get(j));
                            list2.remove(j);
                        } else {
                            ++j;
                        }
                    }
                    tempOriginalImageBlocks = list3;
                }

                float[][] averageBlock1 = calculateAverageOfBlocks(list1);
                float[][] averageBlock2 = calculateAverageOfBlocks(list2);
                newCodeBookBlocks.add(averageBlock1);
                newCodeBookBlocks.add(averageBlock2);
            }
            codeBookBlocks = newCodeBookBlocks;
        }
    }

    // convert float array to int array
    public int[][] convertFloatArrayToIntArray(float[][] doubleArray) {
        int[][] intArray = new int[blockWidth][blockHeight];
        for (int i = 0; i < blockHeight; ++i) {
            for (int j = 0; j < blockWidth; ++j) {
                intArray[i][j] = (int) doubleArray[i][j];
            }
        }
        return intArray;
    }

    // generate binary code (label) for each block in the code book
    public Map<String, int[][]> generateCodeBook(int codebookSize) {
        Map<String, int[][]> codeBook = new java.util.HashMap<>();
        int numberOfBits = (int) Math.ceil(Math.log(codebookSize) / Math.log(2));
        for (int i = 0; i < codebookSize; ++i) {
            String binary = Integer.toBinaryString(i);
            while (binary.length() < numberOfBits) {
                binary = "0" + binary;
            }
            codeBook.put(binary, convertFloatArrayToIntArray(codeBookBlocks.get(i)));
        }
        return codeBook;
    }

    // get the binary code (label) of a block
    public String getBinaryCode(int[][] block) {
        int minDistance = Integer.MAX_VALUE;
        String binaryCode = "";
        for (Map.Entry<String, int[][]> entry : codeBook.entrySet()) {
            int distance = getDistance(block, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                binaryCode = entry.getKey();
            }
        }
        return binaryCode;
    }

    // write the all needed data in a file
    public void writeDataInFile() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("F:\\DC\\Vector-Quantization-TECHNIQUE\\compressed.txt");
        fileOutputStream.write(width);
        fileOutputStream.write(height);
        fileOutputStream.write(blockWidth);
        fileOutputStream.write(blockHeight);
        fileOutputStream.write(codebookSize);
        // write the code book (label, block)
        for (Map.Entry<String, int[][]> entry : codeBook.entrySet()) {
            fileOutputStream.write(Integer.parseInt(entry.getKey(), 2));
            for (int i = 0; i < blockHeight; ++i) {
                for (int j = 0; j < blockWidth; ++j) {
                    fileOutputStream.write(entry.getValue()[i][j]);
                }
            }
        }
        // write the binary code of each block in the original image in a string
        String compressedStream = "";
        for (int i = 0; i < originalImageBlocks.size(); ++i) {
            String binaryCode = getBinaryCode(originalImageBlocks.get(i));
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

    // Function to compress the original data
    public void compress(String fileName) throws IOException {
        this.codebookSize = 4;
        this.originalImage = read2DArray(fileName);
        this.width = originalImage[0].length;
        this.height = originalImage.length;
        this.blockWidth = 2;
        this.blockHeight = 2;
        this.originalImageBlocks = divideImageIntoBlocks(originalImage, blockWidth, blockHeight);
        LBG();
        this.codeBook = generateCodeBook(codebookSize);
        writeDataInFile();
    }
}