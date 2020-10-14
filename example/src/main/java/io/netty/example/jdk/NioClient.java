package io.netty.example.jdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author jjzhou
 * @date 2020/10/14 8:59 上午
 * @description
 */
public class NioClient {

    private int port;

    private String hostname;

    private Selector selector;

    private SocketChannel clientSocketChannel;

    private List<String> respQueue;

    private CountDownLatch connected = new CountDownLatch(1);

    public NioClient(int port, String hostname) {
        this.port = port;
        this.hostname = hostname;
    }

    public void start() throws IOException, InterruptedException {
        // 1. 打开Client SocketChannel
        clientSocketChannel = SocketChannel.open();
        // 2. 配合为非阻塞
        clientSocketChannel.configureBlocking(false);
        // 3. 创建Selector
        selector = Selector.open();
        // 4. 注册Client SocketChannel到Selector
        clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
        // 5. 连接服务器
        clientSocketChannel.connect(new InetSocketAddress(this.hostname, this.port));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handlerKeys();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (connected.getCount() != 0) {
            connected.await();
        }
        System.out.println("Client 已经启动完成...");
    }

    private void handlerKeys() throws IOException {
    }

}
