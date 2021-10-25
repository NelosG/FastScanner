import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class FastScannerSecond implements Closeable {

    private final Reader reader;
    private final String lineSeparator = System.lineSeparator();

    private final char[] buffer = new char[1024];
    private int pos = 0;
    private int size = 0;
    private Integer rollback = null;

    private enum Token {
        INT,
        WORD,
        LINE
    }


    public FastScannerSecond(InputStream in) {
        reader = new InputStreamReader(in);
    }

    public FastScannerSecond(String in) {
        reader = new StringReader(in);
    }

    public FastScannerSecond(File in) throws IOException {
        reader = new FileReader(in, StandardCharsets.UTF_8);
    }

    public boolean hasNextChar() throws IOException {
        return pos < size || read();
    }

    public char nextChar() throws IOException {
        if (hasNextChar()) {
            return buffer[pos++];
        }
        throw new NoSuchElementException("Has no next Character.");
    }

    public boolean hasNextLine() throws IOException {
        return hasNextChar();
    }

    public boolean hasNextInt() throws IOException {
        rollback = pos;
        boolean res = nextToken(Token.INT) != null;
        pos = rollback;
        rollback = null;
        return res;
    }

    public boolean hasNextWord() throws IOException {
        while (hasNextChar() && Character.isWhitespace(buffer[pos])) {
            pos++;
        }
        return hasNextChar();
    }

    public String nextLine() throws IOException {
        String res = nextToken(Token.LINE);
        if (res == null) {
            throw new InputMismatchException("Has no next Line.");
        }
        return res;
    }

    public String nextWord() throws IOException {
        String res = nextToken(Token.WORD);
        if (res == null) {
            throw new InputMismatchException("Has no next Word.");
        }
        return res;
    }

    public Integer nextInt() throws IOException {
        String res = nextToken(Token.INT);
        if (res == null) {
            throw new InputMismatchException("Has no next Int.");
        }
        return Integer.parseInt(res);
    }

    private String nextToken(Token token) throws IOException {
        if (!prevCheck(token)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        while (hasNextChar() && checkLetter(buffer[pos], sb.length(), token)) {
            sb.append(buffer[pos]);
            pos++;
        }
        Character c = null;
        if (hasNextChar()) {
            c = buffer[pos];
        }
        try {
            return checkAndGetResult(sb.toString(), c, token);
        } finally {
            postAction(token);
        }
    }

    private void postAction(Token token) throws IOException {
        switch (token) {
            case LINE -> {
                int i = 0;
                while (i < lineSeparator.length() && hasNextChar() && lineSeparator.charAt(i) == buffer[pos]) {
                    pos++;
                    i++;
                }
                if (i < lineSeparator.length()) {
                    throw new InputMismatchException("Wrong line separator.");
                }
            }
            case INT, WORD -> {
            }
            default -> throw new IllegalStateException("Unexpected value: " + token);
        }
    }


    private boolean prevCheck(Token token) throws IOException {
        switch (token) {
            case WORD, INT -> {
                if (hasNextWord()) return true;
            }
            case LINE -> {
                if (hasNextLine()) return true;
            }
            default -> throw new IllegalStateException("Unexpected value: " + token);
        }
        return false;
    }

    private boolean checkLetter(char c, int posInTokenValue, Token token) {
        boolean res;
        switch (token) {
            case INT -> {
                res = posInTokenValue == 0 && (c == '-' || c == '+');
                res = res || Character.isDigit(c);
                res = res && posInTokenValue < 12;
            }
            case WORD -> res = !Character.isWhitespace(c);
            case LINE -> res = c != lineSeparator.charAt(0);
            default -> throw new IllegalStateException("Unexpected value: " + token);
        }
        return res;
    }

    private String checkAndGetResult(String str, Character trailing, Token token) {
        switch (token) {
            case INT -> {
                if (str.length() == 0 || trailing != null && !Character.isWhitespace(trailing) ||
                        (str.charAt(0) == '-' || str.charAt(0) == '+') && str.length() == 1) {
                    return null;
                }
            }
            case WORD -> {
                if (str.length() == 0 || trailing != null && !Character.isWhitespace(trailing)) {
                    return null;
                }
            }
            case LINE -> {
                if (trailing != null && trailing != lineSeparator.charAt(0)) {
                    return null;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + token);
        }
        return str;
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }

    private boolean read() throws IOException {
        if (rollback == null) {
            size = reader.read(buffer);
            pos = 0;
            return size != -1;
        }
        if (rollback != 0) {
            moveToBeginning();
        }
        int readLength = reader.read(buffer, size, buffer.length - size);
        if (readLength == -1) {
            return false;
        }
        size += readLength;
        return true;
    }

    private void moveToBeginning() {
        for (int i = rollback, j = 0; i < buffer.length; i++, j++) {
            buffer[j] = buffer[i];
        }
        size -= rollback;
        pos -= rollback;
        rollback = 0;
    }
}
