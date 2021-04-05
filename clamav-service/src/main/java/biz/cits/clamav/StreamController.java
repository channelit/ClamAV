package biz.cits.clamav;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class StreamController {

    private static final int CHUNK_SIZE = 2048;
    private static final int DEFAULT_TIMEOUT = 500;
    private static final int PONG_REPLY_LEN = 4;

    @Value("${clamav.host}")
    private String HOST;

    @Value("${clamav.port}")
    private int PORT;

    @EventListener(ApplicationReadyEvent.class)
    public void check() throws Exception {
        System.out.println(ping());
        final InputStream inputStream =
                new DataInputStream(new ByteArrayInputStream("Some String from file".getBytes()));
        String s = new String((scan(inputStream)), StandardCharsets.UTF_8);
        System.out.println(s);
    }
    public byte[] scan(InputStream is) throws Exception {
        try (Socket s = new Socket(HOST,PORT); OutputStream outs = new BufferedOutputStream(s.getOutputStream())) {
            s.setSoTimeout(DEFAULT_TIMEOUT);

            // handshake
            outs.write(asBytes("zINSTREAM\0"));
            outs.flush();
            byte[] chunk = new byte[CHUNK_SIZE];

            try (InputStream clamIs = s.getInputStream()) {
                // send data
                int read = is.read(chunk);
                while (read >= 0) {
                    // The format of the chunk is: '<length><data>' where <length> is the size of the following data in bytes expressed as a 4 byte unsigned
                    // integer in network byte order and <data> is the actual chunk. Streaming is terminated by sending a zero-length chunk.
                    byte[] chunkSize = ByteBuffer.allocate(4).putInt(read).array();

                    outs.write(chunkSize);
                    outs.write(chunk, 0, read);
                    if (clamIs.available() > 0) {
                        // reply from server before scan command has been terminated.
                        byte[] reply = assertSizeLimit(readAll(clamIs));
                        throw new IOException("Scan aborted. Reply from server: " + new String(reply, StandardCharsets.US_ASCII));
                    }
                    read = is.read(chunk);
                }

                // terminate scan
                outs.write(new byte[]{0,0,0,0});
                outs.flush();
                // read reply
                return assertSizeLimit(readAll(clamIs));
            }
        }
    }
    public boolean ping() throws IOException {
        System.out.println("host=" + HOST + " port=" + PORT);
        try (Socket s = new Socket(HOST,PORT); OutputStream outs = s.getOutputStream()) {
            s.setSoTimeout(DEFAULT_TIMEOUT);
            outs.write(asBytes("zPING\0"));
            outs.flush();
            byte[] b = new byte[PONG_REPLY_LEN];
            InputStream inputStream = s.getInputStream();
            int copyIndex = 0;
            int readResult;
            do {
                readResult = inputStream.read(b, copyIndex, Math.max(b.length - copyIndex, 0));
                copyIndex += readResult;
            } while (readResult > 0);
            return Arrays.equals(b, asBytes("PONG"));
        }
    }
    /**
     * Scans bytes for virus by passing the bytes to clamav
     *
     * @param in data to scan
     * @return server reply
     **/
    public byte[] scan(byte[] in) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(in);
        return scan(bis);
    }
    private byte[] assertSizeLimit(byte[] reply) throws Exception {
        String r = new String(reply, StandardCharsets.US_ASCII);
        if (r.startsWith("INSTREAM size limit exceeded."))
            throw new Exception("Clamd size limit exceeded. Full reply from server: " + r);
        return reply;
    }
    // reads all available bytes from the stream
    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();

        byte[] buf = new byte[2000];
        int read = 0;
        do {
            read = is.read(buf);
            tmp.write(buf, 0, read);
        } while ((read > 0) && (is.available() > 0));
        return tmp.toByteArray();
    }
    // byte conversion based on ASCII character set regardless of the current system locale
    private static byte[] asBytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }
}
