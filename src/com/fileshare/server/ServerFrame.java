package com.fileshare.server;

import com.fileshare.database.DatabaseConnection;
import com.fileshare.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Graphical Management Console for the FileShare Server.
 */
public class ServerFrame extends JFrame implements FileServer.ServerListener {

    private JTextField portField;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private DefaultListModel<String> clientsListModel;
    private JList<String> clientsList;
    private JLabel clientsCountLabel;
    private JTextArea logTextArea;

    private FileServer server;

    public ServerFrame() {
        setTitle(Constants.APP_NAME + " - Server Control Panel");
        setSize(850, 550);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        initUI();
        initEvents();
    }

    private void initUI() {
        // Try System Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // --- Top Control Panel ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        configPanel.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(Constants.DEFAULT_PORT), 6);
        configPanel.add(portField);

        startButton = new JButton("Start Server");
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD));
        startButton.setBackground(new Color(46, 125, 50));
        startButton.setForeground(Color.WHITE);
        configPanel.add(startButton);

        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        stopButton.setBackground(new Color(198, 40, 40));
        stopButton.setForeground(Color.WHITE);
        configPanel.add(stopButton);

        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusLabel = new JLabel("Status: STOPPED");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        statusLabel.setForeground(new Color(180, 0, 0));
        statusWrapper.add(statusLabel);

        topPanel.add(configPanel, BorderLayout.WEST);
        topPanel.add(statusWrapper, BorderLayout.EAST);

        // --- Center Split Pane ---
        // Left: Connected Clients
        JPanel clientsPanel = new JPanel(new BorderLayout(5, 5));
        clientsPanel.setPreferredSize(new Dimension(220, 300));
        clientsPanel.setBorder(BorderFactory.createTitledBorder("Online Clients"));

        clientsCountLabel = new JLabel("Clients connected: 0", SwingConstants.CENTER);
        clientsCountLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        clientsPanel.add(clientsCountLabel, BorderLayout.NORTH);

        clientsListModel = new DefaultListModel<>();
        clientsList = new JList<>(clientsListModel);
        clientsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientsList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane clientsScroll = new JScrollPane(clientsList);
        clientsPanel.add(clientsScroll, BorderLayout.CENTER);

        // Right: Log Console
        JPanel logsPanel = new JPanel(new BorderLayout(5, 5));
        logsPanel.setBorder(BorderFactory.createTitledBorder("Server Activity Log"));

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logTextArea.setBackground(new Color(248, 249, 250));
        JScrollPane logScroll = new JScrollPane(logTextArea);
        logsPanel.add(logScroll, BorderLayout.CENTER);

        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogBtn = new JButton("Clear Logs");
        clearLogBtn.addActionListener(e -> logTextArea.setText(""));
        logButtonPanel.add(clearLogBtn);
        logsPanel.add(logButtonPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientsPanel, logsPanel);
        splitPane.setDividerLocation(230);
        splitPane.setContinuousLayout(true);

        // --- Bottom Info Bar ---
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(new EmptyBorder(5, 5, 0, 5));
        JLabel dbInfo = new JLabel("Database: " + DatabaseConnection.getDbUrl());
        dbInfo.setForeground(Color.DARK_GRAY);
        dbInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottomBar.add(dbInfo, BorderLayout.WEST);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void initEvents() {
        // Start Server Action
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServerAction();
            }
        });

        // Stop Server Action
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServerAction();
            }
        });

        // Window Closing Listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server != null && server.isRunning()) {
                    int choice = JOptionPane.showConfirmDialog(
                            ServerFrame.this,
                            "Server is currently running. Stopping will disconnect all clients. Continue?",
                            "Exit FileShare Server",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        server.stop();
                        dispose();
                        System.exit(0);
                    }
                } else {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private void startServerAction() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid port number between 1024 and 65535.", "Invalid Port", JOptionPane.ERROR_MESSAGE);
            return;
        }

        server = new FileServer(port);
        server.addListener(this);

        try {
            server.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not start server: " + ex.getMessage(), "Start Failure", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServerAction() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    // --- FileServer.ServerListener Callbacks ---

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }

    @Override
    public void onClientsChanged(List<String> onlineUsernames) {
        SwingUtilities.invokeLater(() -> {
            clientsListModel.clear();
            for (String u : onlineUsernames) {
                clientsListModel.addElement(u);
            }
            clientsCountLabel.setText("Clients connected: " + onlineUsernames.size());
        });
    }

    @Override
    public void onServerStateChanged(boolean isRunning) {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(!isRunning);
            stopButton.setEnabled(isRunning);
            portField.setEditable(!isRunning);

            if (isRunning) {
                statusLabel.setText("Status: RUNNING on Port " + portField.getText());
                statusLabel.setForeground(new Color(46, 125, 50));
            } else {
                statusLabel.setText("Status: STOPPED");
                statusLabel.setForeground(new Color(198, 40, 40));
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame();
            frame.setVisible(true);
        });
    }
}
