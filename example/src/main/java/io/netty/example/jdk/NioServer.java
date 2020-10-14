package io.netty.example.jdk;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author jjzhou
 * @date 2020/10/13 10:21 下午
 * @description
 */
public class NioServer {

    private int port;

    private ServerSocketChannel serverSocketChannel;

    private Selector selector;

    public NioServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        // 1. 打开ServerSocketChannel
        serverSocketChannel = ServerSocketChannel.open();
        // 2. 配置成非阻塞
        serverSocketChannel.configureBlocking(false);
        // 3. 绑定端口
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(new InetSocketAddress(this.port));
        // 4. 创建Selector
        selector = Selector.open();
        // 5. 注册ServerSocketChannel到Selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server启动完成......");

        handlerKeys();
    }

    private void handlerKeys() throws IOException {
        for (;;) {
            //  通过Selector选择Channel
            int selectNums = selector.select();
            if (0 == selectNums) {
                continue;
            }
            System.out.println("选择channel的数量: " + selectNums);

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
        // 接受连接就绪
        if (key.isAcceptable()) {
            handlerAcceptableKey(key);
        }
        // 读就绪
        if (key.isReadable()) {
            handlerReadableKey(key);
        }
        // 写就绪
        if (key.isWritable()) {
            handlerWritableKey(key);
        }
    }

    private void handlerWritableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        List<String> respQueue = (ArrayList<String>) key.attachment();
        for (String content : respQueue) {
            System.out.println("写入数据： " + content);
            CodecUtil.write(clientSocketChannel, content);
        }
        respQueue.clear();
        clientSocketChannel.register(selector, SelectionKey.OP_READ, respQueue);
    }

    private void handlerReadableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
        if (null == readBuffer) {
            System.out.println("断开channel");
            // ops=0 表示取消注册
            clientSocketChannel.register(selector, 0);
            return;
        }
        if (readBuffer.position() > 0) {
            String content = CodecUtil.newString(readBuffer);
            System.out.println("读取数据: " + content);

            List<String> respQueue = (ArrayList<String>) key.attachment();
            respQueue.add("响应： " + content);
            clientSocketChannel.register(selector, SelectionKey.OP_WRITE, key.attachment());
        }


    }

    private void handlerAcceptableKey(SelectionKey key) throws IOException {
        // 接受 客户端连接
        SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
        clientSocketChannel.configureBlocking(false);
        System.out.println("接受新的channel");
        // 注册ClientSocketChannel到Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ, new ArrayList<String>());
    }

    public static void main(String[] args) throws Exception {
        NioServer server = new NioServer(8080);
        server.start();
    }
}
