import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class VQApp {
    private Frame frame;
    private Label headerLabel;
    private Panel buttonPanel;
    private Button browseButton;
    private Button compressButton;
    private Button decompressButton;
    private Label outputLabel;
    private File selectedFile;
    private VectorQuantizationLBG vq = new VectorQuantizationLBG();

    public VQApp() {
        frame = new Frame("VECTOR QUANTIZATION Compression/Decompression");
        frame.setSize(600, 400);
        frame.setLayout(new GridLayout(3, 1));

        // Center the window on the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) (screenSize.getWidth() - frame.getWidth()) / 2;
        int centerY = (int) (screenSize.getHeight() - frame.getHeight()) / 2;
        frame.setLocation(centerX, centerY);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        // Header Label
        headerLabel = new Label("VECTOR QUANTIZATION TECHNIQUE");
        headerLabel.setAlignment(Label.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // Button Panel
        buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20)); // Centered layout with spacing

        browseButton = new Button("Browse");
        compressButton = new Button("Compress");
        decompressButton = new Button("Decompress");

        browseButton.setPreferredSize(new Dimension(120, 30));
        compressButton.setPreferredSize(new Dimension(120, 30));
        decompressButton.setPreferredSize(new Dimension(120, 30));

        browseButton.setFont(new Font("Arial", Font.PLAIN, 16));
        compressButton.setFont(new Font("Arial", Font.PLAIN, 16));
        decompressButton.setFont(new Font("Arial", Font.PLAIN, 16));

        browseButton.setFocusable(false);
        compressButton.setFocusable(false);
        decompressButton.setFocusable(false);

        browseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        compressButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        decompressButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        buttonPanel.add(browseButton);
        buttonPanel.add(compressButton);
        buttonPanel.add(decompressButton);
        buttonPanel.setFont(new Font("Arial", Font.PLAIN, 16));
        buttonPanel.setForeground(Color.BLACK);

        // Output Label
        outputLabel = new Label();
        outputLabel.setAlignment(Label.CENTER);
        outputLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        frame.add(headerLabel);
        frame.add(buttonPanel);
        frame.add(outputLabel);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileDialog fileDialog = new FileDialog(frame, "Select a File", FileDialog.LOAD);
                fileDialog.setVisible(true);
                String filename = fileDialog.getFile();
                if (filename != null) {
                    selectedFile = new File(fileDialog.getDirectory(), filename);
                    outputLabel.setText("Selected file: " + selectedFile.getName());
                }
            }
        });

        compressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    try {
                        vq.compress(selectedFile.getAbsolutePath());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    outputLabel.setText("Compression completed. Check 'compressed.txt'.");
                } else {
                    outputLabel.setText("Please select a file to compress.");
                }
            }
        });

        decompressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    try {
                        vq.decompress(selectedFile.getAbsolutePath());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    outputLabel.setText("Decompression completed. Check 'decompressed.txt'.");
                } else {
                    outputLabel.setText("Please select a file to decompress.");
                }
            }
        });

        frame.setVisible(true);
    }
}
