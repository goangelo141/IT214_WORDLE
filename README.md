# IT214_WORDLE

A desktop word-guessing game built with Java Swing. Find the hidden five-letter
word in six guesses. Green tiles are correct letters in the correct position,
yellow tiles are in the word in a different position, and gray tiles are not
in the word.

## Run

With a JDK installed, compile and launch from this folder:

```text
javac main.java
java main
```

Use the keyboard or the on-screen keys to enter a guess. Press Enter to submit,
Backspace or DEL to remove a letter, and **New game** to start over. Guesses
must be in [words.txt](words.txt), which provides both possible answers and
valid guesses. Add five-letter words to that file, separated by spaces or
newlines. Blank lines and lines beginning with `#` are ignored. Entries are
case-insensitive; duplicates are ignored.

Keep `words.txt` in the folder where you run `java main`. If the file is
missing or contains an invalid entry, the game displays an error on startup.