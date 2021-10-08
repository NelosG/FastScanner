import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

public class FastScanner implements AutoCloseable {
    //:TODO write documentation

    //Our Reader
    private final Reader is;


    // State Fields
    private boolean EOF = false;
    private boolean closed = false;


    // Fields for buffer
    private final int NORMAL_BUFFER_SIZE = 300;

    private char[] buffer;
    private int pos;
    private int len;
    private int bufferSize = NORMAL_BUFFER_SIZE;
    private int rollback_count = 0;

    // Support Field for nextInt and NextLong
    private long foundedNextLong;


    //Constructors

    public FastScanner(InputStream in) {
        this(in, StandardCharsets.UTF_8);
    }

    public FastScanner(InputStream in, Charset charset) {
        buffer = new char[bufferSize];
        is = new InputStreamReader(in, charset);
    }

    public FastScanner(String in) {
        this(in, StandardCharsets.UTF_8);
    }

    public FastScanner(String in, Charset charset) {
        this(new ByteArrayInputStream((in).getBytes(charset)), charset);
    }

    public FastScanner(File in) throws IOException {
        this(in, StandardCharsets.UTF_8);
    }

    public FastScanner(File in, Charset charset) throws IOException {
        this(new FileInputStream(in), charset);
    }


    // Checks our State
    private void checkState() {
        if (closed) {
            throw new IllegalStateException("FastScanner is closed");
        }
    }


    // Check, that we have some nextValue

    public boolean hasNextChar() throws IOException {
        checkState();
        if (pos < len) {
            return true;
        }
        if (is.ready()) {
            readBuffer();
        } else {
            return false;
        }
        return !EOF;
    }

    public boolean hasNextLine() throws IOException {
        return hasNextChar();
    }

    public boolean hasNext() throws IOException {
        skipBlank(true);
        boolean res = hasNextChar();
        rollback();
        return res;
    }

    public boolean hasNextInt() throws IOException {
        return hasNextIntOrLongImpl(true, true);
    }

    public boolean hasNextLong() throws IOException {
        return hasNextIntOrLongImpl(true, false);
    }

    private boolean hasNextIntOrLongImpl(boolean isOnlyCheck, boolean intNotLong) throws IOException {
        skipBlank(isOnlyCheck);
        char c;
        int maxSymbols = intNotLong ? 10 : 19;
        if (hasNextChar()) {
            incRollback(isOnlyCheck);
            c = nextChar();

        } else {
            if (!isOnlyCheck) {
                throw new NoSuchElementException();
            }
            return false;
        }
        StringBuilder sb = new StringBuilder();
        if (c == '-' || c == '+') {
            sb.append(c);
            incRollback(isOnlyCheck);
            c = nextChar();
            ++maxSymbols;
        }
        if (!Character.isDigit(c)) {
            rollback();
            return false;
        }


        boolean flag = false;
        while (Character.isDigit(c)) {
            sb.append(c);
            if (sb.length() > maxSymbols) {
                rollback();
                return false;
            }
            if (hasNextChar()) {
                incRollback(isOnlyCheck);
                c = nextChar();
            } else {
                flag = true;
                break;
            }
        }

        rollback();

        if (!flag && !Character.isWhitespace(c)) {
            return false;
        } else {
            try {
                if (intNotLong) {
                    foundedNextLong = Integer.parseInt(sb.toString());
                } else {
                    foundedNextLong = Long.parseLong(sb.toString());
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    // Get some nextValue

    public char nextChar() throws IOException {
        checkState();
        if (!hasNextChar()) {
            throw new NoSuchElementException("End of file");
        }
        return buffer[pos++];
    }

    public int nextInt() throws IOException {
        if (hasNextIntOrLongImpl(false, true)) {
            return (int) foundedNextLong;
        }
        throw new InputMismatchException();
    }

    public int nextLong() throws IOException {
        if (hasNextIntOrLongImpl(false, false)) {
            return (int) foundedNextLong;
        }
        throw new InputMismatchException();
    }

    public String nextLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c;
        while (hasNextChar()) {
            c = nextChar();
            if (c == '\r') {
                if (hasNextChar()) {
                    incRollback(true); // to use on old MacOS
                    if (nextChar() != '\n') {
                        rollback();
                    }
                }
                break;
            } else if (c == '\n') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String next() throws IOException {
        skipBlank(false);
        StringBuilder sb = new StringBuilder();
        char c;
        while (hasNextChar()) {
            if (hasNextChar()) {
                incRollback(true);
                c = nextChar();
            } else {

                break;
            }
            if (c == '\n' && sb.length() != 0) {
                rollback();
                break;
            }
            --rollback_count;
            if (c != ' ') {
                sb.append(c);
            } else {
                break;
            }
        }
        if (sb.length() != 0) {
            return sb.toString();
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        is.close();
    }

    // Support Function
    private void skipBlank(boolean wantToRollback) throws IOException {
        while (hasNextChar()) {
            ++rollback_count;
            if (!Character.isWhitespace(nextChar())) {
                --pos;
                --rollback_count;
                break;
            }
            if (!wantToRollback) {
                --rollback_count;
            }
        }
    }

    // Work with buffer

    //TODO::Bad complex function, need to fix
    private void readBuffer() throws IOException {
        if (rollback_count > 0) {
            if (pos >= bufferSize - 2) {
                bufferSize *= 2;
                char[] tempBuffer = new char[bufferSize];
                len = len - pos + rollback_count;
                System.arraycopy(buffer, pos - rollback_count, tempBuffer, 0, len);
                pos = rollback_count;
                buffer = tempBuffer;
                int prevLen = len;
                len = 0;
                while (len == 0) {
                    len = is.read(buffer, prevLen, bufferSize - prevLen);
                }
                if (len == -1) {
                    EOF = true;
                    len = 0;
                }
                len += prevLen;
                return;
            } else {
                System.arraycopy(buffer, bufferSize - rollback_count, buffer, 0, rollback_count);
            }
        }
        len = 0;
        while (len == 0) {
            len = is.read(buffer, rollback_count, bufferSize - rollback_count);
        }
        if (len == -1) {
            EOF = true;
        }
        len += rollback_count;
        pos = rollback_count;

        if (len < bufferSize / 2) {
            int prevBuffSize = bufferSize;
            bufferSize = Math.max(len, NORMAL_BUFFER_SIZE);
            if (prevBuffSize != bufferSize) {
                char[] tempBuffer = new char[bufferSize];
                System.arraycopy(buffer, 0, tempBuffer, 0, len);
                buffer = tempBuffer;
            }
        }
    }

    private void incRollback(boolean wantToRollback) {
        if (wantToRollback) {
            ++rollback_count;
        }
    }

    private void rollback() {
        pos -= rollback_count;
        rollback_count = 0;
    }
}
