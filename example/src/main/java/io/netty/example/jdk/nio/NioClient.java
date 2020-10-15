package io.netty.example.jdk.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
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

    private List<String> respQueue = new ArrayList<String>();

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
        for (;;) {
            int selectNums = selector.select();
            if (0 == selectNums) {
                continue;
            }
            System.out.println("client选择channel数量：" + selectNums);

            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator<SelectionKey> keyIt = selectionKeySet.iterator();
            while (keyIt.hasNext()) {
                SelectionKey key = keyIt.next();
                keyIt.remove();
                if (!key.isValid()) {
                    continue;
                }
                handlerKey(key);
            }
        }
    }

    private void handlerKey(SelectionKey key) throws IOException {
        if (null == key) {
            return;
        }
        // 接受连接就绪
        if (key.isConnectable()) {
            handlerConnectable(key);
        }
        // 读就绪
        if (key.isReadable()) {
            handlerReadable(key);
        }
        // 写就绪
        if (key.isWritable()) {
            handlerWriteable(key);
        }
    }

    private void handlerConnectable(SelectionKey key) throws IOException {
        // 完成连接
        if (!clientSocketChannel.isConnectionPending()) {
            return;
        }
        clientSocketChannel.finishConnect();

        System.out.println("接受新的channel");
        clientSocketChannel.register(selector, SelectionKey.OP_READ, respQueue);

        //标记为已连接
        connected.countDown();
    }

    @SuppressWarnings("unchecked")
    private void handlerWriteable(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        List<String> responseQueue = (ArrayList<String>) key.attachment();
        for (String content : responseQueue) {
            System.out.println("client writable写入数据：" + content);
            CodecUtil.write(clientSocketChannel, content);
        }
        respQueue.clear();

        clientSocketChannel.register(selector, SelectionKey.OP_READ, respQueue);
    }

    @SuppressWarnings("unchecked")
    private void handlerReadable(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = CodecUtil.read(clientSocketChannel);

        if (null == buffer) {
            System.out.println("断开channel");
            clientSocketChannel.register(selector, 0);
            return;
        }

        if (buffer.position() > 0) {
            String content = CodecUtil.newString(buffer);
            System.out.println("client readable读取数据：" + content);

//            List<String> responseQueue = (ArrayList<String>) key.attachment();
//            responseQueue.add("client readable响应：" + content);
//            clientSocketChannel.register(selector, SelectionKey.OP_WRITE, key.attachment());
        }
    }

    public synchronized void send(String content) throws ClosedChannelException {
        // 添加到响应队列
        respQueue.add(content);

        System.out.println("send写入数据：" + content);

        clientSocketChannel.register(selector, SelectionKey.OP_WRITE, respQueue);
        selector.wakeup();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        NioClient client = new NioClient(8080, "localhost");
        client.start();
        for (int i = 0; i < 30; i ++) {
            client.send("test: " + i);
            Thread.sleep(1000);
        }
    }


}
