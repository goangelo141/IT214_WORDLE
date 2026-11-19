import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class main {
    private static final int WORD_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 6;

    private static final Color BACKGROUND = new Color(18, 18, 19);
    private static final Color TILE_EMPTY = new Color(18, 18, 19);
    private static final Color TILE_BORDER = new Color(58, 58, 60);
    private static final Color TILE_ABSENT = new Color(58, 58, 60);
    private static final Color TILE_PRESENT = new Color(181, 159, 59);
    private static final Color TILE_CORRECT = new Color(83, 141, 78);
    private static final Color KEY_DEFAULT = new Color(129, 131, 132);
    private static final Font TILE_FONT = new Font("Arial", Font.BOLD, 30);

    private final JFrame frame = new JFrame("Word Guess");
    private final JLabel statusLabel = new JLabel("Guess the 5-letter word", SwingConstants.CENTER);
    private final JLabel[][] tiles = new JLabel[MAX_ATTEMPTS][WORD_LENGTH];
    private final TileState[][] tileStates = new TileState[MAX_ATTEMPTS][WORD_LENGTH];
    private final Map<Character, JButton> letterKeys = new HashMap<Character, JButton>();
    private final Map<Character, TileState> keyboardStates = new HashMap<Character, TileState>();
    private final List<String> words;
    private final Set<String> validWords;
    private final Random random = new Random();
    private final StringBuilder currentGuess = new StringBuilder();

    private String answer;
    private int attempts;
    private boolean gameOver;

    private enum TileState {
        EMPTY, ABSENT, PRESENT, CORRECT
    }

    public main(List<String> words) {
        this.words = words;
        this.validWords = new HashSet<String>(words);
        buildInterface();
        startNewGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception exception) {
                    System.err.println("Could not apply the system look and feel: " + exception.getMessage());
                }
                try {
                    new main(loadWords(Paths.get("words.txt")));
                } catch (IOException exception) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Could not load words.txt: " + exception.getMessage(),
                        "Word list error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(1);
                }
            }
        });
    }

    static List<String> loadWords(Path path) throws IOException {
        List<String> loadedWords = new ArrayList<String>();
        Set<String> uniqueWords = new HashSet<String>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (lineNumber == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] entries = line.split("\\s+");
            for (String entry : entries) {
                if (!entry.matches("[a-zA-Z]{5}")) {
                    throw new IOException(
                        "Invalid entry on line " + (lineNumber + 1)
                            + " (expected a 5-letter word): " + entry
                    );
                }
                String word = entry.toLowerCase(Locale.ROOT);
                if (uniqueWords.add(word)) {
                    loadedWords.add(word);
                }
            }
        }

        if (loadedWords.isEmpty()) {
            throw new IOException("the file does not contain any 5-letter words.");
        }
        return Collections.unmodifiableList(loadedWords);
    }

    private void buildInterface() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(390, 650));

        JPanel content = new JPanel();
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        JLabel title = new JLabel("WORD GUESS", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 25));
        header.add(title, BorderLayout.CENTER);

        JButton newGameButton = new JButton("New game");
        newGameButton.setFocusable(false);
        newGameButton.addActionListener(event -> startNewGame());
        header.add(newGameButton, BorderLayout.EAST);
        content.add(header);
        content.add(Box.createVerticalStrut(16));

        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        statusLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(14));

        JPanel board = new JPanel(new GridLayout(MAX_ATTEMPTS, WORD_LENGTH, 6, 6));
        board.setBackground(BACKGROUND);
        board.setPreferredSize(new Dimension(330, 390));
        board.setMaximumSize(new Dimension(330, 390));
        board.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        for (int row = 0; row < MAX_ATTEMPTS; row++) {
            for (int column = 0; column < WORD_LENGTH; column++) {
                JLabel tile = new JLabel("", SwingConstants.CENTER);
                tile.setOpaque(true);
                tile.setBackground(TILE_EMPTY);
                tile.setForeground(Color.WHITE);
                tile.setFont(TILE_FONT);
                tile.setBorder(BorderFactory.createLineBorder(TILE_BORDER, 2));
                tiles[row][column] = tile;
                tileStates[row][column] = TileState.EMPTY;
                board.add(tile);
            }
        }
        content.add(board);
        content.add(Box.createVerticalStrut(24));

        content.add(createKeyboardRow("QWERTYUIOP"));
        content.add(Box.createVerticalStrut(7));
        content.add(createKeyboardRow("ASDFGHJKL"));
        content.add(Box.createVerticalStrut(7));
        content.add(createKeyboardRow("ZXCVBNM", true));

        frame.setContentPane(content);
        installKeyboardBindings();
        frame.pack();
        frame.setSize(460, 690);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createKeyboardRow(String letters) {
        return createKeyboardRow(letters, false);
    }

    private JPanel createKeyboardRow(String letters, boolean includeControls) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setBackground(BACKGROUND);
        if (includeControls) {
            JButton enter = createKeyButton("ENTER", 63);
            enter.addActionListener(event -> submitGuess());
            row.add(enter);
        }

        for (int index = 0; index < letters.length(); index++) {
            final char letter = letters.charAt(index);
            JButton key = createKeyButton(String.valueOf(letter), 34);
            letterKeys.put(letter, key);
            key.addActionListener(event -> addLetter(letter));
            row.add(key);
        }

        if (includeControls) {
            JButton delete = createKeyButton("DEL", 54);
            delete.addActionListener(event -> removeLetter());
            row.add(delete);
        }
        return row;
    }

    private JButton createKeyButton(String text, int width) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, 46));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusable(false);
        button.setFont(new Font("Arial", Font.BOLD, text.length() > 1 ? 10 : 13));
        button.setForeground(Color.WHITE);
        button.setBackground(KEY_DEFAULT);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void installKeyboardBindings() {
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            final char keyLetter = letter;
            String actionName = "type-" + letter;
            frame.getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("pressed " + letter), actionName);
            frame.getRootPane().getActionMap().put(actionName, new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent event) {
                    addLetter(keyLetter);
                }
            });
        }

        frame.getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("pressed BACK_SPACE"), "delete-letter");
        frame.getRootPane().getActionMap().put("delete-letter", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                removeLetter();
            }
        });

        frame.getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("pressed ENTER"), "submit-guess");
        frame.getRootPane().getActionMap().put("submit-guess", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                submitGuess();
            }
        });
    }

    private void startNewGame() {
        answer = words.get(random.nextInt(words.size())).toUpperCase(Locale.ROOT);
        attempts = 0;
        gameOver = false;
        currentGuess.setLength(0);
        keyboardStates.clear();

        for (int row = 0; row < MAX_ATTEMPTS; row++) {
            for (int column = 0; column < WORD_LENGTH; column++) {
                tileStates[row][column] = TileState.EMPTY;
                updateTile(row, column, '\0');
            }
        }
        for (Map.Entry<Character, JButton> entry : letterKeys.entrySet()) {
            entry.getValue().setBackground(KEY_DEFAULT);
        }
        setStatus("Guess the 5-letter word", Color.WHITE);
    }

    private void addLetter(char letter) {
        if (gameOver || currentGuess.length() >= WORD_LENGTH) {
            return;
        }
        currentGuess.append(letter);
        renderCurrentGuess();
    }

    private void removeLetter() {
        if (gameOver || currentGuess.length() == 0) {
            return;
        }
        currentGuess.deleteCharAt(currentGuess.length() - 1);
        renderCurrentGuess();
    }

    private void renderCurrentGuess() {
        int row = attempts;
        for (int column = 0; column < WORD_LENGTH; column++) {
            char letter = column < currentGuess.length() ? currentGuess.charAt(column) : '\0';
            updateTile(row, column, letter);
        }
    }

    private void submitGuess() {
        if (gameOver) {
            return;
        }
        if (currentGuess.length() != WORD_LENGTH) {
            setStatus("Enter 5 letters before submitting", new Color(235, 190, 80));
            return;
        }

        String guess = currentGuess.toString().toLowerCase(Locale.ROOT);
        if (!validWords.contains(guess)) {
            setStatus("That word is not in the word list", new Color(235, 190, 80));
            return;
        }

        TileState[] result = evaluateGuess(guess, answer.toLowerCase(Locale.ROOT));
        for (int column = 0; column < WORD_LENGTH; column++) {
            tileStates[attempts][column] = result[column];
            updateTile(attempts, column, currentGuess.charAt(column));
            updateKeyboard(currentGuess.charAt(column), result[column]);
        }

        attempts++;
        currentGuess.setLength(0);

        if (guess.equals(answer.toLowerCase(Locale.ROOT))) {
            gameOver = true;
            setStatus("Great job! You got it.", Color.WHITE);
        } else if (attempts == MAX_ATTEMPTS) {
            gameOver = true;
            setStatus("The word was " + answer, Color.WHITE);
        } else {
            setStatus("Try again  •  " + (MAX_ATTEMPTS - attempts) + " guesses left", Color.WHITE);
        }
    }

    private TileState[] evaluateGuess(String guess, String target) {
        TileState[] result = new TileState[WORD_LENGTH];
        int[] remainingLetters = new int[26];

        for (int index = 0; index < WORD_LENGTH; index++) {
            result[index] = TileState.ABSENT;
            if (guess.charAt(index) == target.charAt(index)) {
                result[index] = TileState.CORRECT;
            } else {
                remainingLetters[target.charAt(index) - 'a']++;
            }
        }

        for (int index = 0; index < WORD_LENGTH; index++) {
            if (result[index] != TileState.CORRECT) {
                int letterIndex = guess.charAt(index) - 'a';
                if (remainingLetters[letterIndex] > 0) {
                    result[index] = TileState.PRESENT;
                    remainingLetters[letterIndex]--;
                }
            }
        }
        return result;
    }

    private void updateKeyboard(char letter, TileState state) {
        TileState existing = keyboardStates.get(letter);
        if (existing == TileState.CORRECT || (existing == TileState.PRESENT && state == TileState.ABSENT)) {
            return;
        }
        keyboardStates.put(letter, state);
        JButton key = letterKeys.get(letter);
        if (key != null) {
            key.setBackground(colorForState(state));
        }
    }

    private void updateTile(int row, int column, char letter) {
        JLabel tile = tiles[row][column];
        TileState state = tileStates[row][column];
        tile.setText(letter == '\0' ? "" : String.valueOf(letter));
        tile.setBackground(colorForState(state));
        tile.setBorder(BorderFactory.createLineBorder(
            state == TileState.EMPTY ? TILE_BORDER : colorForState(state), 2));
    }

    private Color colorForState(TileState state) {
        switch (state) {
            case CORRECT:
                return TILE_CORRECT;
            case PRESENT:
                return TILE_PRESENT;
            case ABSENT:
                return TILE_ABSENT;
            default:
                return TILE_EMPTY;
        }
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
}
