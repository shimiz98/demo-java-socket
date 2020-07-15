package study.socket.util;

import java.util.Optional;

public class MainArgs {
    public MainArgs(String[] args) {
        // TODO
    }

    public String serverAddr() {
        return "172.217.25.206";
    }

    public int serverPort() {
        return 80;
    }

    public String clientAddr() {
        return null;
    }

    public int clientPort() {
        return 0;
    }

    public int connectTimeout() {
        return 11 * 1000;
    }

    public Optional<Integer> soTimeout() {
        return Optional.of(15 * 1000);
    }

    public int writeTimeout() {
        return 15 * 1000;
    }


}
