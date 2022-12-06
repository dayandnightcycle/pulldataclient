package com.xazktx.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;

@Slf4j
@Component
public class FileTransferServer {
    @Value("${socket.port}")
    private int port; // 服务端端口
    @Value("${oracle.username}")
    private String username;
    @Value("${oracle.password}")
    private String password;
    @Value("${oracle.instance}")
    private String instance;
    @Value("${oracle.schemas}")
    private String schemas;
    @Value("${oracle.directory_name}")
    private String directory_name;
    @Value("${oracle.dirertory_path}")
    private String directory_path;
    @Value("${oracle.exclude}")
    private String exclude;
    private static DecimalFormat df = null;
    //private String commond = "cmd /c start impdp cssxk/cssxk@orcl DIRECTORY=DATA_PUMP_DIR DUMPFILE=daochu.dmp " +


    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }

    //public FileTransferServer() throws Exception {
    //    super(port);
    //}

    /**
     * 使用线程处理每个客户端传输的文件
     *
     * @throws Exception
     */
    public void load() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            // server尝试接收其他Socket的连接请求，server的accept方法是阻塞式的
            System.out.println(port);
            Socket socket = serverSocket.accept();
            /**
             * 我们的服务端处理客户端的连接请求是同步进行的， 每次接收到来自客户端的连接请求后，
             * 都要先跟当前的客户端通信完之后才能再处理下一个连接请求。 这在并发比较多的情况下会严重影响程序的性能，
             * 为此，我们可以把它改为如下这种异步处理与客户端通信的方式
             */
            // 每接收到一个Socket就建立一个新的线程来处理它
            new Thread(new Task(socket)).start();
        }
    }

    /**
     * 处理客户端传输过来的文件线程类
     */
    class Task implements Runnable {

        private Socket socket;

        private DataInputStream dis;

        private FileOutputStream fos;

        public Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());

                // 文件名和长度
                String fileName = dis.readUTF();
                long fileLength = dis.readLong();
                File directory = new File(directory_path);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                fos = new FileOutputStream(file);

                // 开始接收文件
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                    fos.write(bytes, 0, length);
                    fos.flush();
                }
                log.info("======== 文件接收成功 [File Name：" + fileName + "] [Size：" + getFormatFileSize(fileLength) + "] ========");
                cmd();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                    if (dis != null)
                        dis.close();
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void cmd() throws IOException, InterruptedException {
        String c = "cmd /c start impdp FLO/flo@orcl DIRECTORY=DATA_PUMP_DIR DUMPFILE=daochu.dmp SCHEMAS=FLO TABLE_EXISTS_ACTION=replace parallel=8";
        String commond = "cmd /c start impdp " + username + "/" + password + "@" + instance + " DIRECTORY=" + directory_name + " DUMPFILE=daochu.dmp " +
                "SCHEMAS=" + schemas + " TABLE_EXISTS_ACTION=replace parallel=8 " + exclude;
        System.out.println(commond);
        log.info("dmp导入开始执行");
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(commond);
        InputStream inputStream = process.getInputStream();
        log.info("inputStream.available() = " + inputStream.available());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
        String s;
        StringBuffer sb = new StringBuffer();
        while ((s = bufferedReader.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        System.out.println("sb " + sb);
        int i = process.waitFor();
        log.info("i = " + i);
        log.info("dmp导入完成");
        bufferedReader.close();
        process.destroy();
    }

    /**
     * 格式化文件大小
     *
     * @param length
     * @return
     */
    private String getFormatFileSize(long length) {
        double size = ((double) length) / (1 << 30);
        if (size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if (size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if (size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }
}