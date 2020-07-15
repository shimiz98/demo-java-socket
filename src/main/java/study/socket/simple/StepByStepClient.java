package study.socket.simple;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import study.socket.util.MainArgs;

public class StepByStepClient {
    private DateTimeFormatter OUTPUT_FILENAME_FORMATTER = DateTimeFormatter
            .ofPattern("'http-res_'uuuuMMdd-HHmmss'.txt'");
    private MainArgs mainArgs;

    public static void main(String[] args) {
        new StepByStepClient(args).run();
    }

    public StepByStepClient(String[] args) {
        mainArgs = new MainArgs(args);
    }

    public void run() {
        StringBuilder sb = new StringBuilder();
        try (Socket socket = new Socket()) {

            // ソケットオプションを設定する
            if (mainArgs.soTimeout().isPresent()) {
                socket.setSoTimeout(mainArgs.soTimeout().get());
            }

            // ローカルアドレスとローカルポートを指定する
            InetSocketAddress clientSockAddr;
            if (mainArgs.clientAddr() == null) {
                clientSockAddr = new InetSocketAddress(mainArgs.clientPort());
            } else {
                clientSockAddr = new InetSocketAddress(mainArgs.clientAddr(), mainArgs.clientPort());
            }
            socket.bind(clientSockAddr);

            // 接続する
            socket.connect(new InetSocketAddress(mainArgs.serverAddr(), mainArgs.serverPort()),
                    mainArgs.connectTimeout());

            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            sb.setLength(0);
            sb.append("GET /index.html HTTP/1.0\r\n");
            sb.append("Connection: close\r\n");
            sb.append("\r\n");

            try (WriteTimeoutMonitor writeTimeoutMonitor = new WriteTimeoutMonitor(socket, mainArgs.writeTimeout())) {
                ;
                os.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
            }

            List<String> httpHeaders = new ArrayList<>();
            sb.setLength(0);

            HTTP_HEADER_LOOP: while (true) {
                int b = is.read();
                if (b == -1) {
                    break;
                }

                switch (b) {
                case -1:
                    throw new RuntimeException("");
                case '\r':
                    // 何もしない
                    break;
                case '\n':
                    if (sb.length() == 0) {
                        // 空行の場合、HTTPヘッダーの終わりを示す
                        break HTTP_HEADER_LOOP;
                    } else {
                        //
                        httpHeaders.add(sb.toString());
                        sb.setLength(0);
                        break;
                    }
                default:
                    // 1文字追加
                    sb.append(b & 0xff);
                }
            }

            Path resBodyPath = Paths.get(OUTPUT_FILENAME_FORMATTER.format(LocalDateTime.now()));
            try (OutputStream fos = Files.newOutputStream(resBodyPath)) {
                byte[] buf = new byte[16];
                int n;
                while ((n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class WriteTimeoutMonitor implements Closeable {
        private volatile boolean closed = false;

        public WriteTimeoutMonitor(Socket s, int timeout) {
            // 別スレッドを生成する
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e) {
                        // TODO ログ出力
                        e.printStackTrace();
                    }

                    if (!closed) {
                        try {
                            // TODO ログ出力
                            s.close();
                        } catch (IOException e) {
                            // TODO ログ出力
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }
}
