package io.netty.example.jdk.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author jjzhou
 * @date 2020/10/14 8:30 上午
 * @description
 */
public class CodecUtil {

    public static ByteBuffer read(SocketChannel channel) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            int count = channel.read(buffer);
            if (-1 == count) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static String newString(ByteBuffer buffer) {
        if (null == buffer) {
            return null;
        }
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), bytes, 0, buffer.remaining());
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void write(SocketChannel channel, String content) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try {
            buffer.put(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        buffer.flip();

        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
