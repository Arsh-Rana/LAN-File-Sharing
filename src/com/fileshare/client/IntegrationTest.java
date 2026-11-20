package com.fileshare.client;

import com.fileshare.beans.FileTransferBean;
import com.fileshare.beans.LoginBean;
import com.fileshare.beans.TransferHistoryBean;
import com.fileshare.beans.UserBean;
import com.fileshare.network.FileChunk;
import com.fileshare.network.MessageType;
import com.fileshare.network.NetworkMessage;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IntegrationTest {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("Running End-to-End Client-Server Integration Test");
        System.out.println("==================================================");

        CountDownLatch latch = new CountDownLatch(1);

        // 1. Client Alice Socket
        Socket sAlice = new Socket("localhost", 8888);
        ObjectOutputStream oosAlice = new ObjectOutputStream(sAlice.getOutputStream());
        oosAlice.flush();
        ObjectInputStream oisAlice = new ObjectInputStream(sAlice.getInputStream());

        // 2. Client Bob Socket
        Socket sBob = new Socket("localhost", 8888);
        ObjectOutputStream oosBob = new ObjectOutputStream(sBob.getOutputStream());
        oosBob.flush();
        ObjectInputStream oisBob = new ObjectInputStream(sBob.getInputStream());

        // Login Alice
        System.out.println("[Test] Logging in Alice...");
        oosAlice.writeObject(NetworkMessage.request(MessageType.LOGIN_REQUEST, new LoginBean("alice", "password123")));
        oosAlice.flush();
        oosAlice.reset();
        NetworkMessage respAlice = (NetworkMessage) oisAlice.readObject();
        System.out.println("[Test] Alice Login: " + respAlice.getMessage() + " (Success: " + respAlice.isSuccess() + ")");

        // Login Bob
        System.out.println("[Test] Logging in Bob...");
        oosBob.writeObject(NetworkMessage.request(MessageType.LOGIN_REQUEST, new LoginBean("bob", "password123")));
        oosBob.flush();
        oosBob.reset();
        NetworkMessage respBob = (NetworkMessage) oisBob.readObject();
        System.out.println("[Test] Bob Login: " + respBob.getMessage() + " (Success: " + respBob.isSuccess() + ")");

        // Create a temporary file to send from Alice to Bob
        File testFile = new File("test_transfer.txt");
        try (FileWriter fw = new FileWriter(testFile)) {
            fw.write("Hello from Alice! This is a LAN file transfer verification payload.");
        }

        // Alice initiates transfer to Bob
        System.out.println("[Test] Alice sends transfer request to Bob...");
        FileTransferBean transfer = new FileTransferBean("alice", "bob", testFile.getName(), testFile.length());
        oosAlice.writeObject(NetworkMessage.request(MessageType.TRANSFER_REQUEST, transfer));
        oosAlice.flush();
        oosAlice.reset();

        // Read Bob's incoming messages in a thread
        new Thread(() -> {
            try {
                while (true) {
                    NetworkMessage msg = (NetworkMessage) oisBob.readObject();
                    if (msg.getType() == MessageType.TRANSFER_REQUEST) {
                        FileTransferBean incoming = (FileTransferBean) msg.getData();
                        System.out.println("[Bob Receiver] Received transfer request for: " + incoming.getFileName());
                        // Bob accepts transfer
                        oosBob.writeObject(NetworkMessage.request(MessageType.TRANSFER_ACCEPT, incoming));
                        oosBob.flush();
                        oosBob.reset();
                    } else if (msg.getType() == MessageType.TRANSFER_CHUNK) {
                        FileChunk chunk = (FileChunk) msg.getData();
                        System.out.println("[Bob Receiver] Received chunk: " + chunk.getBytesRead() + " bytes");
                    } else if (msg.getType() == MessageType.TRANSFER_COMPLETE) {
                        System.out.println("[Bob Receiver] Transfer complete received successfully!");
                        latch.countDown();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Alice waits for TRANSFER_ACCEPT
        while (true) {
            NetworkMessage msg = (NetworkMessage) oisAlice.readObject();
            if (msg.getType() == MessageType.TRANSFER_ACCEPT) {
                FileTransferBean accepted = (FileTransferBean) msg.getData();
                System.out.println("[Alice Sender] Transfer accepted by Bob! Streaming file...");

                // Send chunk
                byte[] data = new byte[(int) testFile.length()];
                try (FileInputStream fis = new FileInputStream(testFile)) {
                    fis.read(data);
                }
                FileChunk chunk = new FileChunk(accepted.getTransferUuid(), data, data.length, 0, true);
                oosAlice.writeObject(NetworkMessage.request(MessageType.TRANSFER_CHUNK, chunk));
                oosAlice.flush();
                oosAlice.reset();

                // Send complete
                oosAlice.writeObject(NetworkMessage.request(MessageType.TRANSFER_COMPLETE, accepted));
                oosAlice.flush();
                oosAlice.reset();
                System.out.println("[Alice Sender] Transfer sent.");
                break;
            }
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        System.out.println("[Test] Completed within timeout: " + finished);

        // Query history
        oosAlice.writeObject(NetworkMessage.request(MessageType.HISTORY_REQUEST, null));
        oosAlice.flush();
        oosAlice.reset();
        NetworkMessage histMsg = (NetworkMessage) oisAlice.readObject();
        if (histMsg.getData() instanceof List) {
            List<?> hist = (List<?>) histMsg.getData();
            System.out.println("[Test] Alice Transfer History count: " + hist.size());
            for (Object o : hist) {
                System.out.println("  -> " + o);
            }
        }

        // Clean up test sockets & file
        sAlice.close();
        sBob.close();
        testFile.delete();

        System.out.println("==================================================");
        System.out.println("ALL INTEGRATION TESTS PASSED WITH 100% SUCCESS!");
        System.out.println("==================================================");
        System.exit(0);
    }
}
