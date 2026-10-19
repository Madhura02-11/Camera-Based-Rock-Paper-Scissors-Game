import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main extends JFrame {

    // =========================================================
    // COLORS
    // =========================================================

    private static final Color BG = new Color(3, 10, 28);
    private static final Color PANEL = new Color(7, 20, 46);
    private static final Color CARD = new Color(13, 39, 76);
    private static final Color CARD_DARK = new Color(10, 30, 59);

    private static final Color BLUE = new Color(25, 150, 255);
    private static final Color CYAN = new Color(70, 210, 255);
    private static final Color GREEN = new Color(40, 235, 120);
    private static final Color RED = new Color(255, 80, 110);
    private static final Color ORANGE = new Color(255, 170, 55);

    private static final Color WHITE = new Color(245, 248, 255);
    private static final Color GREY = new Color(175, 190, 215);


    // =========================================================
    // MEDIAPIPE CONNECTION
    // =========================================================

    private Socket socket;
    private DataInputStream input;
    private Thread mediaPipeThread;

    private volatile byte[] latestImage;

    private volatile String currentGesture = "Unknown";

    private volatile int handX = 0;
    private volatile int handY = 0;
    private volatile int handWidth = 0;
    private volatile int handHeight = 0;


    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private CameraPanel cameraPanel;

    private JLabel gestureValueLabel;
    private JLabel coordinateLabel;

    private JLabel yourGestureLabel;
    private JLabel computerGestureLabel;

    private JLabel yourIconLabel;
    private JLabel computerIconLabel;

    private JLabel resultLabel;
    private JLabel roundLabel;

    private JLabel playerScoreLabel;
    private JLabel computerScoreLabel;

    private JLabel statusLabel;

    private JPanel historyPanel;


    // =========================================================
    // GAME VARIABLES
    // =========================================================

    private int playerScore = 0;
    private int computerScore = 0;
    private int round = 0;

    private final Random random = new Random();

    // SQLite database helper
    private GameDatabase database;

    private final String[] choices = {
            "Rock",
            "Paper",
            "Scissors"
    };

    private final List<String> history = new ArrayList<>();


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Main() {

        // Initialize SQLite database
        database = new GameDatabase();

        setTitle("Camera Rock Paper Scissors");

        setSize(1350, 800);

        setMinimumSize(
                new Dimension(1100, 700)
        );

        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        getContentPane().setBackground(BG);

        createInterface();

        connectToMediaPipe();

        addWindowListener(
                new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(
                            java.awt.event.WindowEvent e) {

                        closeConnection();
                    }
                }
        );
    }


    // =========================================================
    // MAIN INTERFACE
    // =========================================================

    private void createInterface() {

        JPanel root = new JPanel(
                new BorderLayout(0, 8)
        );

        root.setBackground(BG);

        root.setBorder(
                new EmptyBorder(
                        10, 15, 8, 15
                )
        );


        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        JPanel header = new JPanel(
                new BorderLayout()
        );

        header.setOpaque(false);


        JLabel title = new JLabel(
                "<html>" +
                "<font color='#F5F8FF'>✋ Camera </font>" +
                "<font color='#FFD166'>Rock</font>" +
                "<font color='#55D6FF'> Paper</font>" +
                "<font color='#9BAEFF'> Scissors</font>" +
                "</html>"
        );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        29
                )
        );

        header.add(
                title,
                BorderLayout.WEST
        );


        // MediaPipe badge

        JPanel mediaBadge = new RoundedPanel(
                new Color(8, 32, 58),
                new Color(35, 120, 190)
        );

        mediaBadge.setLayout(
                new FlowLayout(
                        FlowLayout.CENTER,
                        6,
                        5
                )
        );


        JLabel mediaIcon = new JLabel("◉");

        mediaIcon.setForeground(CYAN);

        mediaIcon.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );


        statusLabel = new JLabel(
                "MediaPipe Connecting..."
        );

        statusLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        statusLabel.setForeground(GREY);


        mediaBadge.add(mediaIcon);
        mediaBadge.add(statusLabel);


        header.add(
                mediaBadge,
                BorderLayout.EAST
        );


        root.add(
                header,
                BorderLayout.NORTH
        );


        // -----------------------------------------------------
        // MAIN TWO PANELS
        // -----------------------------------------------------

        JPanel main = new JPanel(
                new GridLayout(
                        1,
                        2,
                        12,
                        0
                )
        );

        main.setOpaque(false);


        main.add(
                createCameraSection()
        );


        main.add(
                createGameSection()
        );


        root.add(
                main,
                BorderLayout.CENTER
        );


        // -----------------------------------------------------
        // GAME HISTORY
        // -----------------------------------------------------

        root.add(
                createHistorySection(),
                BorderLayout.SOUTH
        );


        add(root);
    }


    // =========================================================
    // CAMERA SECTION
    // =========================================================

    private JPanel createCameraSection() {

        JPanel panel = new RoundedPanel(
                PANEL,
                BLUE
        );

        panel.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        panel.setBorder(
                new EmptyBorder(
                        8, 8, 7, 8
                )
        );


        // -----------------------------------------------------
        // CAMERA HEADER
        // -----------------------------------------------------

        JPanel top = new JPanel(
                new BorderLayout()
        );

        top.setOpaque(false);


        JPanel live = new RoundedPanel(
                new Color(7, 39, 64),
                BLUE
        );

        live.setLayout(
                new FlowLayout(
                        FlowLayout.CENTER,
                        6,
                        3
                )
        );


        JLabel dot = new JLabel("●");

        dot.setForeground(GREEN);


        JLabel liveText = new JLabel(
                "LIVE CAMERA"
        );

        liveText.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        13
                )
        );

        liveText.setForeground(WHITE);


        live.add(dot);
        live.add(liveText);


        top.add(
                live,
                BorderLayout.WEST
        );


        JLabel tracking = new JLabel(
                "● TRACKING"
        );

        tracking.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        11
                )
        );

        tracking.setForeground(GREEN);


        top.add(
                tracking,
                BorderLayout.EAST
        );


        panel.add(
                top,
                BorderLayout.NORTH
        );


        // -----------------------------------------------------
        // CAMERA DISPLAY
        // -----------------------------------------------------

        cameraPanel = new CameraPanel();

        cameraPanel.setPreferredSize(
                new Dimension(
                        620,
                        430
                )
        );


        panel.add(
                cameraPanel,
                BorderLayout.CENTER
        );


        // -----------------------------------------------------
        // CAMERA BOTTOM
        // -----------------------------------------------------

        JPanel bottom = new JPanel(
                new BorderLayout()
        );

        bottom.setOpaque(false);


        gestureValueLabel = new JLabel(
                "✋ DETECTED: Unknown"
        );

        gestureValueLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        15
                )
        );

        gestureValueLabel.setForeground(GREY);


        coordinateLabel = new JLabel(
                "X: 0   Y: 0   W: 0   H: 0"
        );

        coordinateLabel.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        11
                )
        );

        coordinateLabel.setForeground(GREY);


        bottom.add(
                gestureValueLabel,
                BorderLayout.WEST
        );

        bottom.add(
                coordinateLabel,
                BorderLayout.EAST
        );


        panel.add(
                bottom,
                BorderLayout.SOUTH
        );


        return panel;
    }


    // =========================================================
    // GAME SECTION
    // =========================================================

    private JPanel createGameSection() {

        JPanel panel = new RoundedPanel(
                PANEL,
                new Color(35, 100, 180)
        );

        panel.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        panel.setBorder(
                new EmptyBorder(
                        10, 12, 9, 12
                )
        );


        
        // =====================================================
        // TOP AREA
        // =====================================================

        JPanel top = new JPanel(
                new BorderLayout()
        );

        top.setOpaque(false);


        roundLabel = new JLabel(
                "🏆 Round: 0"
        );

        roundLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        roundLabel.setForeground(WHITE);


        top.add(
                roundLabel,
                BorderLayout.WEST
        );


        JPanel heading = new JPanel();

        heading.setOpaque(false);

        heading.setLayout(
                new BoxLayout(
                        heading,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel ready = new JLabel(
                "Ready to Play?",
                SwingConstants.CENTER
        );

        ready.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );

        ready.setForeground(WHITE);

        ready.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        JLabel subtitle = new JLabel(
                "I'll catch your gesture!",
                SwingConstants.CENTER
        );

        subtitle.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        14
                )
        );

        subtitle.setForeground(GREY);

        subtitle.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        heading.add(ready);

        heading.add(
                Box.createVerticalStrut(2)
        );

        heading.add(subtitle);


        top.add(
                heading,
                BorderLayout.CENTER
        );


        panel.add(
                top,
                BorderLayout.NORTH
        );


        // =====================================================
        // GESTURE CARDS
        // =====================================================

        JPanel cards = new JPanel(
                new GridBagLayout()
        );

        cards.setOpaque(false);


        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridy = 0;

        gbc.insets =
                new Insets(
                        5, 5, 5, 5
                );

        gbc.fill =
                GridBagConstraints.NONE;

        gbc.anchor =
                GridBagConstraints.CENTER;


        // -----------------------------------------------------
        // YOUR CARD
        // -----------------------------------------------------

        gbc.gridx = 0;


        JPanel playerCard =
                createPlayerCard();


        playerCard.setPreferredSize(
                new Dimension(
                        190,
                        220
                )
        );


        cards.add(
                playerCard,
                gbc
        );


        // -----------------------------------------------------
        // VS
        // -----------------------------------------------------

        gbc.gridx = 1;


        JPanel vsPanel = new JPanel(
                new GridBagLayout()
        );

        vsPanel.setOpaque(false);

        vsPanel.setPreferredSize(
                new Dimension(
                        70,
                        220
                )
        );


        JLabel vs = new JLabel("VS");

        vs.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        32
                )
        );

        vs.setForeground(ORANGE);


        vsPanel.add(vs);


        cards.add(
                vsPanel,
                gbc
        );


        // -----------------------------------------------------
        // COMPUTER CARD
        // -----------------------------------------------------

        gbc.gridx = 2;


        JPanel computerCard =
                createComputerCard();


        computerCard.setPreferredSize(
                new Dimension(
                        190,
                        220
                )
        );


        cards.add(
                computerCard,
                gbc
        );


        panel.add(
                cards,
                BorderLayout.CENTER
        );


        // =====================================================
        // THIS WAS MISSING IN YOUR CODE
        // =====================================================

        JPanel bottom = new JPanel();

        bottom.setOpaque(false);

        bottom.setLayout(
                new BoxLayout(
                        bottom,
                        BoxLayout.Y_AXIS
                )
        );


        // -----------------------------------------------------
        // RESULT
        // -----------------------------------------------------

        resultLabel = new JLabel(
                "Ready to Play?",
                SwingConstants.CENTER
        );

        resultLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        resultLabel.setForeground(CYAN);

        resultLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        bottom.add(
                resultLabel
        );


        bottom.add(
                Box.createVerticalStrut(4)
        );


        // -----------------------------------------------------
        // PLAY BUTTON
        // -----------------------------------------------------

        JButton play =
                createPlayButton();


        play.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        play.addActionListener(
                e -> playGame()
        );


        bottom.add(play);


        bottom.add(
                Box.createVerticalStrut(4)
        );


        // -----------------------------------------------------
        // SCORE
        // -----------------------------------------------------

        JPanel score = new JPanel(
                new GridLayout(
                        1,
                        2
                )
        );

        score.setOpaque(false);


        playerScoreLabel =
                createScoreLabel(
                        "YOU   0"
                );


        computerScoreLabel =
                createScoreLabel(
                        "COMPUTER   0"
                );


        score.add(
                playerScoreLabel
        );

        score.add(
                computerScoreLabel
        );


        bottom.add(score);


        bottom.add(
                Box.createVerticalStrut(4)
        );

        // -----------------------------------------------------
        // PREVIOUS GAME RECORDS BUTTON
        // -----------------------------------------------------

        JButton recordsButton = new JButton(
                "VIEW PREVIOUS RECORDS"
        );

        recordsButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        recordsButton.setFocusPainted(false);
        recordsButton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        13
                )
        );

        recordsButton.setForeground(WHITE);
        recordsButton.setBackground(CARD);
        recordsButton.setBorder(
                BorderFactory.createLineBorder(
                        BLUE,
                        1
                )
        );

        recordsButton.setPreferredSize(
                new Dimension(
                        240,
                        38
                )
        );

        recordsButton.addActionListener(
                e -> showPreviousRecords()
        );

        bottom.add(recordsButton);


        bottom.add(
                Box.createVerticalStrut(4)
        );


        // =====================================================
        // STEPS
        // =====================================================

        JPanel steps = new RoundedPanel(
                new Color(
                        11,
                        31,
                        61
                ),
                new Color(
                        40,
                        75,
                        125
                )
        );


        steps.setLayout(
                new GridLayout(
                        1,
                        3
                )
        );


        steps.setPreferredSize(
                new Dimension(
                        500,
                        48
                )
        );


        steps.setMaximumSize(
                new Dimension(
                        650,
                        48
                )
        );


        steps.add(
                createStep(
                        "👋",
                        "1. Detect Hand"
                )
        );


        steps.add(
                createStep(
                        "▶",
                        "2. Tap PLAY"
                )
        );


        steps.add(
                createStep(
                        "🏆",
                        "3. See Result"
                )
        );


        bottom.add(steps);


        // -----------------------------------------------------
        // ADD BOTTOM PANEL
        // -----------------------------------------------------

        panel.add(
                bottom,
                BorderLayout.SOUTH
        );


        return panel;
    }


    // =========================================================
    // PLAYER CARD
    // =========================================================

    private JPanel createPlayerCard() {

        JPanel card = new RoundedPanel(
                CARD,
                BLUE
        );

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBorder(
                new EmptyBorder(
                        10, 5, 8, 5
                )
        );


        JLabel title = new JLabel(
                "Your Gesture",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        13
                )
        );

        title.setForeground(WHITE);

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        card.add(title);


        card.add(
                Box.createVerticalStrut(5)
        );


        yourIconLabel =
                createEmojiLabel("❓");


        card.add(
                yourIconLabel
        );


        yourGestureLabel = new JLabel(
                "Unknown",
                SwingConstants.CENTER
        );

        yourGestureLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        yourGestureLabel.setForeground(GREEN);

        yourGestureLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        card.add(
                yourGestureLabel
        );


        return card;
    }


    // =========================================================
    // COMPUTER CARD
    // =========================================================

    private JPanel createComputerCard() {

        JPanel card = new RoundedPanel(
                CARD_DARK,
                new Color(
                        60,
                        95,
                        160
                )
        );

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBorder(
                new EmptyBorder(
                        10, 5, 8, 5
                )
        );


        JLabel title = new JLabel(
                "Computer",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        13
                )
        );

        title.setForeground(WHITE);

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        card.add(title);


        card.add(
                Box.createVerticalStrut(5)
        );


        computerIconLabel =
                createEmojiLabel("❓");


        card.add(
                computerIconLabel
        );


        computerGestureLabel = new JLabel(
                "Choosing...",
                SwingConstants.CENTER
        );

        computerGestureLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        computerGestureLabel.setForeground(CYAN);

        computerGestureLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        card.add(
                computerGestureLabel
        );


        return card;
    }


    // =========================================================
    // EMOJI LABEL
    // =========================================================

    private JLabel createEmojiLabel(
            String emoji
    ) {

        JLabel label = new JLabel(
                emoji,
                SwingConstants.CENTER
        );


        label.setFont(
                new Font(
                        "Segoe UI Emoji",
                        Font.PLAIN,
                        48
                )
        );


        label.setForeground(WHITE);


        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        label.setPreferredSize(
                new Dimension(
                        120,
                        75
                )
        );


        label.setMinimumSize(
                new Dimension(
                        120,
                        75
                )
        );


        label.setMaximumSize(
                new Dimension(
                        140,
                        75
                )
        );


        return label;
    }


    // =========================================================
    // STEP
    // =========================================================

    private JPanel createStep(
            String icon,
            String text
    ) {

        JPanel panel = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        4,
                        13
                )
        );

        panel.setOpaque(false);


        JLabel iconLabel =
                new JLabel(icon);

        iconLabel.setFont(
                new Font(
                        "Segoe UI Emoji",
                        Font.PLAIN,
                        15
                )
        );


        JLabel textLabel =
                new JLabel(text);

        textLabel.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        11
                )
        );

        textLabel.setForeground(WHITE);


        panel.add(iconLabel);
        panel.add(textLabel);


        return panel;
    }


    // =========================================================
    // PLAY BUTTON
    // =========================================================

    private JButton createPlayButton() {

        JButton button = new JButton(
                "<html>" +
                "<center>" +
                "<font size='5'>▶</font><br>" +
                "<b>TAP TO<br>PLAY</b>" +
                "</center>" +
                "</html>"
        );


        button.setPreferredSize(
                new Dimension(
                        145,
                        105
                )
        );


        button.setMinimumSize(
                new Dimension(
                        145,
                        105
                )
        );


        button.setMaximumSize(
                new Dimension(
                        145,
                        105
                )
        );


        button.setForeground(WHITE);

        button.setBackground(
                new Color(
                        8,
                        42,
                        85
                )
        );


        button.setFocusPainted(false);


        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                CYAN,
                                2
                        ),
                        new EmptyBorder(
                                8,
                                18,
                                8,
                                18
                        )
                )
        );


        return button;
    }


    // =========================================================
    // SCORE LABEL
    // =========================================================

    private JLabel createScoreLabel(
            String text
    ) {

        JLabel label = new JLabel(
                text,
                SwingConstants.CENTER
        );


        label.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );


        label.setForeground(WHITE);


        return label;
    }


    // =========================================================
    // HISTORY
    // =========================================================

    private JPanel createHistorySection() {

        JPanel panel = new RoundedPanel(
                PANEL,
                new Color(
                        35,
                        75,
                        135
                )
        );


        panel.setPreferredSize(
                new Dimension(
                        0,
                        82
                )
        );


        panel.setLayout(
                new BorderLayout(
                        3,
                        2
                )
        );


        panel.setBorder(
                new EmptyBorder(
                        5, 10, 5, 10
                )
        );


        JLabel title = new JLabel(
                "◴  Game History"
        );


        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );


        title.setForeground(WHITE);


        panel.add(
                title,
                BorderLayout.NORTH
        );


        historyPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        7,
                        2
                )
        );


        historyPanel.setOpaque(false);


        panel.add(
                historyPanel,
                BorderLayout.CENTER
        );


        return panel;
    }


    // =========================================================
    // CONNECT TO MEDIAPIPE
    // =========================================================

    private void connectToMediaPipe() {

        try {

            socket = new Socket(
                    "127.0.0.1",
                    5000
            );


            input = new DataInputStream(
                    socket.getInputStream()
            );


            statusLabel.setText(
                    "MediaPipe Connected"
            );


            statusLabel.setForeground(
                    GREEN
            );


            mediaPipeThread =
                    new Thread(
                            this::receiveData
                    );


            mediaPipeThread.start();


        } catch (IOException e) {

            statusLabel.setText(
                    "MediaPipe Offline"
            );


            statusLabel.setForeground(
                    RED
            );


            System.out.println(
                    "MediaPipe Error: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // RECEIVE DATA
    // =========================================================

    private void receiveData() {

        try {

            while (
                    socket != null
                            &&
                    !socket.isClosed()
            ) {

                int jsonLength =
                        input.readInt();


                if (
                        jsonLength <= 0
                                ||
                        jsonLength > 1000000
                ) {

                    break;
                }


                byte[] jsonBytes =
                        new byte[jsonLength];


                input.readFully(
                        jsonBytes
                );


                String json =
                        new String(
                                jsonBytes,
                                StandardCharsets.UTF_8
                        );


                int imageLength =
                        input.readInt();


                if (
                        imageLength <= 0
                                ||
                        imageLength > 10000000
                ) {

                    break;
                }


                byte[] imageBytes =
                        new byte[imageLength];


                input.readFully(
                        imageBytes
                );


                currentGesture =
                        getJsonString(
                                json,
                                "gesture"
                        );


                handX =
                        getJsonInt(
                                json,
                                "x"
                        );


                handY =
                        getJsonInt(
                                json,
                                "y"
                        );


                handWidth =
                        getJsonInt(
                                json,
                                "width"
                        );


                handHeight =
                        getJsonInt(
                                json,
                                "height"
                        );


                latestImage =
                        imageBytes;


                SwingUtilities.invokeLater(
                        this::updateInterface
                );
            }


        } catch (Exception e) {

            System.out.println(
                    "MediaPipe connection stopped."
            );
        }
    }


    // =========================================================
    // UPDATE INTERFACE
    // =========================================================

    private void updateInterface() {

        if (
                latestImage != null
        ) {

            try {

                BufferedImage image =
                        ImageIO.read(
                                new ByteArrayInputStream(
                                        latestImage
                                )
                        );


                if (
                        image != null
                ) {

                    cameraPanel.setImage(
                            image
                    );
                }


            } catch (Exception ignored) {
            }
        }


        gestureValueLabel.setText(
                "✋ DETECTED: "
                        + currentGesture
        );


        if (
                currentGesture.equalsIgnoreCase(
                        "Unknown"
                )
        ) {

            gestureValueLabel.setForeground(
                    GREY
            );

        } else {

            gestureValueLabel.setForeground(
                    GREEN
            );


            yourGestureLabel.setText(
                    currentGesture
            );


            yourIconLabel.setText(
                    getGestureEmoji(
                            currentGesture
                    )
            );
        }


        coordinateLabel.setText(
                "X: "
                        + handX
                        + "   Y: "
                        + handY
                        + "   W: "
                        + handWidth
                        + "   H: "
                        + handHeight
        );
    }


    // =========================================================
    // GESTURE EMOJI
    // =========================================================

    private String getGestureEmoji(
            String gesture
    ) {

        if (
                gesture.equalsIgnoreCase(
                        "Rock"
                )
        ) {

            return "✊";
        }


        if (
                gesture.equalsIgnoreCase(
                        "Paper"
                )
        ) {

            return "✋";
        }


        if (
                gesture.equalsIgnoreCase(
                        "Scissors"
                )
        ) {

            return "✌";
        }


        return "❓";
    }


    // =========================================================
    // PLAY GAME
    // =========================================================

    private void playGame() {

        if (currentGesture == null
                || currentGesture.equalsIgnoreCase("Unknown")) {

            resultLabel.setText("Show your hand!");
            resultLabel.setForeground(ORANGE);
            return;
        }

        String player = currentGesture;

        String computer =
                choices[
                        random.nextInt(
                                choices.length
                        )
                ];

        round++;

        roundLabel.setText(
                "🏆 Round: "
                        + round
        );

        // YOUR GESTURE
        yourGestureLabel.setText(player);

        yourIconLabel.setText(
                getGestureEmoji(player)
        );

        // COMPUTER GESTURE
        computerGestureLabel.setText(computer);

        computerIconLabel.setText(
                getGestureEmoji(computer)
        );

        // RESULT
        String result =
                determineWinner(
                        player,
                        computer
                );

        resultLabel.setText(result);

        if (result.equals("YOU WIN!")) {

            playerScore++;
            resultLabel.setForeground(GREEN);

        } else if (result.equals("COMPUTER WINS!")) {

            computerScore++;
            resultLabel.setForeground(RED);

        } else {

            resultLabel.setForeground(CYAN);
        }

        playerScoreLabel.setText(
                "YOU   "
                        + playerScore
        );

        computerScoreLabel.setText(
                "COMPUTER   "
                        + computerScore
        );

        addHistory(
                player,
                computer,
                result
        );

        // Save completed game record into SQLite database.
        int score;

        if (result.equals("YOU WIN!")) {
            score = 1;
        } else if (result.equals("COMPUTER WINS!")) {
            score = -1;
        } else {
            score = 0;
        }

        database.saveGameRecord(
                round,
                player,
                computer,
                result,
                score
        );

        System.out.println(
                "Game saved: Round "
                        + round
                        + " | Player: "
                        + player
                        + " | Computer: "
                        + computer
                        + " | Result: "
                        + result
                        + " | Score: "
                        + score
        );
    }


    // =========================================================
    // WINNER
    // =========================================================

    // =========================================================
    // SHOW PREVIOUS DATABASE RECORDS
    // =========================================================

    private void showPreviousRecords() {

        StringBuilder records = new StringBuilder();

        records.append(
                "ID    ROUND    PLAYER      COMPUTER      RESULT              SCORE\n"
        );

        records.append(
                "--------------------------------------------------------------------------\n"
        );

        boolean hasRecords = false;

        try {

            ResultSet resultSet = database.getAllGameRecords();

            while (resultSet.next()) {

                hasRecords = true;

                int id = resultSet.getInt("id");
                int savedRound = resultSet.getInt("round");
                String player = resultSet.getString("player_choice");
                String computer = resultSet.getString("computer_choice");
                String result = resultSet.getString("result");
                int score = resultSet.getInt("score");

                records.append(
                        String.format(
                                "%-5d %-8d %-11s %-13s %-20s %d\n",
                                id,
                                savedRound,
                                player,
                                computer,
                                result,
                                score
                        )
                );
            }

            // Close the ResultSet and the database resources.
            java.sql.Statement statement =
                    resultSet.getStatement();

            java.sql.Connection connection =
                    statement.getConnection();

            resultSet.close();
            statement.close();
            connection.close();

            if (!hasRecords) {
                records.append(
                        "No previous game records found."
                );
            }

            JTextArea recordsArea = new JTextArea(
                    records.toString()
            );

            recordsArea.setEditable(false);
            recordsArea.setFont(
                    new Font(
                            Font.MONOSPACED,
                            Font.PLAIN,
                            13
                    )
            );

            recordsArea.setBackground(BG);
            recordsArea.setForeground(WHITE);

            JScrollPane scrollPane = new JScrollPane(
                    recordsArea
            );

            scrollPane.setPreferredSize(
                    new Dimension(
                            850,
                            450
                    )
            );

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "Previous Game Records",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load game records.\n\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    private String determineWinner(
            String player,
            String computer
    ) {

        if (
                player.equalsIgnoreCase(
                        computer
                )
        ) {

            return "DRAW!";
        }


        if (
                player.equalsIgnoreCase("Rock")
                        &&
                computer.equalsIgnoreCase("Scissors")
        ) {

            return "YOU WIN!";
        }


        if (
                player.equalsIgnoreCase("Paper")
                        &&
                computer.equalsIgnoreCase("Rock")
        ) {

            return "YOU WIN!";
        }


        if (
                player.equalsIgnoreCase("Scissors")
                        &&
                computer.equalsIgnoreCase("Paper")
        ) {

            return "YOU WIN!";
        }


        return "COMPUTER WINS!";
    }


    // =========================================================
    // ADD HISTORY
    // =========================================================

    private void addHistory(
            String player,
            String computer,
            String result
    ) {

        String text =
                "Round "
                        + round
                        + "   "
                        + getGestureEmoji(player)
                        + " "
                        + player
                        + "  vs  "
                        + getGestureEmoji(computer)
                        + " "
                        + computer
                        + "   "
                        + result;


        history.add(text);


        if (
                history.size() > 5
        ) {

            history.remove(0);
        }


        historyPanel.removeAll();


        for (
                String item : history
        ) {

            JLabel label =
                    new JLabel(item);


            label.setOpaque(true);


            label.setBackground(
                    new Color(
                            12,
                            33,
                            65
                    )
            );


            label.setForeground(WHITE);


            label.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            11
                    )
            );


            label.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(
                                    new Color(
                                            40,
                                            85,
                                            145
                                    )
                            ),
                            new EmptyBorder(
                                    5,
                                    8,
                                    5,
                                    8
                            )
                    )
            );


            historyPanel.add(label);
        }


        historyPanel.revalidate();

        historyPanel.repaint();
    }


    // =========================================================
    // JSON STRING
    // =========================================================

    private String getJsonString(
            String json,
            String key
    ) {

        try {

            String search =
                    "\"" + key + "\"";


            int position =
                    json.indexOf(search);


            if (
                    position < 0
            ) {

                return "Unknown";
            }


            int colon =
                    json.indexOf(
                            ":",
                            position
                    );


            int firstQuote =
                    json.indexOf(
                            "\"",
                            colon + 1
                    );


            int secondQuote =
                    json.indexOf(
                            "\"",
                            firstQuote + 1
                    );


            return json.substring(
                    firstQuote + 1,
                    secondQuote
            );


        } catch (Exception e) {

            return "Unknown";
        }
    }


    // =========================================================
    // JSON INTEGER
    // =========================================================

    private int getJsonInt(
            String json,
            String key
    ) {

        try {

            String search =
                    "\"" + key + "\"";


            int position =
                    json.indexOf(search);


            if (
                    position < 0
            ) {

                return 0;
            }


            int colon =
                    json.indexOf(
                            ":",
                            position
                    );


            int comma =
                    json.indexOf(
                            ",",
                            colon
                    );


            int brace =
                    json.indexOf(
                            "}",
                            colon
                    );


            if (
                    comma == -1
                            ||
                    (
                            brace != -1
                                    &&
                            brace < comma
                    )
            ) {

                comma = brace;
            }


            String value =
                    json.substring(
                            colon + 1,
                            comma
                    ).trim();


            return Integer.parseInt(
                    value
            );


        } catch (Exception e) {

            return 0;
        }
    }


    // =========================================================
    // CLOSE CONNECTION
    // =========================================================

    private void closeConnection() {

        try {

            if (
                    input != null
            ) {

                input.close();
            }


            if (
                    socket != null
            ) {

                socket.close();
            }


        } catch (IOException ignored) {
        }
    }


    // =========================================================
    // ROUNDED PANEL
    // =========================================================

    static class RoundedPanel
            extends JPanel {

        private final Color background;
        private final Color borderColor;


        public RoundedPanel(
                Color background,
                Color borderColor
        ) {

            this.background =
                    background;

            this.borderColor =
                    borderColor;

            setOpaque(false);
        }


        @Override
        protected void paintComponent(
                Graphics g
        ) {

            Graphics2D g2 =
                    (Graphics2D) g.create();


            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );


            g2.setColor(
                    background
            );


            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    20,
                    20
            );


            g2.setColor(
                    borderColor
            );


            g2.setStroke(
                    new BasicStroke(
                            1.5f
                    )
            );


            g2.drawRoundRect(
                    1,
                    1,
                    getWidth() - 3,
                    getHeight() - 3,
                    20,
                    20
            );


            g2.dispose();


            super.paintComponent(g);
        }
    }


    // =========================================================
    // CAMERA PANEL
    // =========================================================

    static class CameraPanel
            extends JPanel {

        private BufferedImage image;


        public CameraPanel() {

            setOpaque(false);
        }


        public void setImage(
                BufferedImage image
        ) {

            this.image = image;

            repaint();
        }


        @Override
        protected void paintComponent(
                Graphics g
        ) {

            super.paintComponent(g);


            Graphics2D g2 =
                    (Graphics2D) g.create();


            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );


            g2.setColor(
                    new Color(
                            2,
                            8,
                            20
                    )
            );


            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    18,
                    18
            );


            if (
                    image != null
            ) {

                int panelWidth =
                        getWidth();

                int panelHeight =
                        getHeight();


                double imageRatio =
                        (double)
                                image.getWidth()
                                /
                                image.getHeight();


                double panelRatio =
                        (double)
                                panelWidth
                                /
                                panelHeight;


                int drawWidth;

                int drawHeight;


                if (
                        imageRatio > panelRatio
                ) {

                    drawWidth =
                            panelWidth;

                    drawHeight =
                            (int)
                                    (
                                            panelWidth
                                                    /
                                            imageRatio
                                    );

                } else {

                    drawHeight =
                            panelHeight;

                    drawWidth =
                            (int)
                                    (
                                            panelHeight
                                                    *
                                            imageRatio
                                    );
                }


                int x =
                        (
                                panelWidth
                                        -
                                drawWidth
                        )
                                / 2;


                int y =
                        (
                                panelHeight
                                        -
                                drawHeight
                        )
                                / 2;


                g2.drawImage(
                        image,
                        x,
                        y,
                        drawWidth,
                        drawHeight,
                        null
                );


            } else {

                g2.setColor(GREY);


                g2.setFont(
                        new Font(
                                "Arial",
                                Font.BOLD,
                                16
                        )
                );


                String text =
                        "Waiting for MediaPipe camera...";


                FontMetrics fm =
                        g2.getFontMetrics();


                int x =
                        (
                                getWidth()
                                        -
                                fm.stringWidth(
                                        text
                                )
                        )
                                / 2;


                int y =
                        (
                                getHeight()
                                        +
                                fm.getAscent()
                        )
                                / 2;


                g2.drawString(
                        text,
                        x,
                        y
                );
            }


            g2.dispose();
        }
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(
                () -> {

                    Main main =
                            new Main();

                    main.setVisible(true);
                }
        );
    }
}