package client.ui.screens;

import client.net.NetworkClient;
import client.ui.ClientApp;
import client.ui.Theme;
import client.ui.components.ModernButton;
import client.ui.components.ModernCard;
import client.ui.components.ModernTextField;
import shared.OpCodes;
import shared.Packet;
import shared.dtos.LoginRequest;
import shared.dtos.LoginResponse;
import shared.dtos.RegisterRequest;
import shared.dtos.RegisterResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Authentication screen providing Login, Account Registration,
 * and Socket Server Connection configuration for HUST Examination Platform.
 */
public class LoginScreen extends JPanel {

    private final ClientApp app;
    private final NetworkClient client;

    // Server Config
    private ModernTextField txtHost;
    private ModernTextField txtPort;
    private JLabel lblConnStatus;

    // Login Form
    private ModernTextField txtLoginUser;
    private ModernTextField.PasswordField txtLoginPass;
    private ModernButton btnLogin;
    private JLabel lblLoginError;

    // Register Form
    private ModernTextField txtRegUser;
    private ModernTextField.PasswordField txtRegPass;
    private ModernTextField txtRegName;
    private ModernTextField txtRegEmail;
    private ModernButton btnRegister;
    private JLabel lblRegStatus;

    public LoginScreen(ClientApp app) {
        this.app = app;
        this.client = app.getClient();

        setLayout(new GridBagLayout());
        setBackground(Theme.BG_DARK);

        initUI();
    }

    private void initUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;

        // Container Card
        ModernCard rootCard = new ModernCard(new BorderLayout(0, 16));
        rootCard.setPreferredSize(new Dimension(520, 640));
        rootCard.setBorder(new EmptyBorder(24, 28, 20, 28));

        // 1. Top Header Banner - HUST Branding
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);

        JPanel pnlBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlBadge.setOpaque(false);
        JLabel badgeHust = Theme.createBadge("HANOI UNIVERSITY OF SCIENCE AND TECHNOLOGY", Theme.HUST_RED, Color.WHITE);
        pnlBadge.add(badgeHust);
        pnlHeader.add(pnlBadge);
        pnlHeader.add(Box.createVerticalStrut(8));

        JLabel lblTitle = new JLabel("HUST ONLINE EXAM PLATFORM", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_GOLD);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(4));

        JLabel lblSub = new JLabel("Cổng Khảo thí & Luyện thi Trực tuyến - ĐHBK Hà Nội", SwingConstants.CENTER);
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlHeader.add(lblSub);

        rootCard.add(pnlHeader, BorderLayout.NORTH);

        // 2. Center Tabs (Login vs Register)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.FONT_BODY_BOLD);
        tabbedPane.setBackground(Theme.BG_SURFACE);
        tabbedPane.setForeground(Theme.TEXT_PRIMARY);

        tabbedPane.addTab("Đăng nhập (Sign In)", createLoginPanel());
        tabbedPane.addTab("Đăng ký mới (Register)", createRegisterPanel());

        rootCard.add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Connection & Quick Accounts Bar
        JPanel pnlBottom = new JPanel(new BorderLayout(0, 10));
        pnlBottom.setOpaque(false);

        // Quick login presets
        JPanel pnlPresets = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        pnlPresets.setOpaque(false);
        JLabel lblQuick = new JLabel("Tài khoản mẫu:");
        lblQuick.setFont(Theme.FONT_SMALL);
        lblQuick.setForeground(Theme.TEXT_MUTED);
        pnlPresets.add(lblQuick);

        String[][] presets = {
                {"student01", "student123", "Sinh viên 1"},
                {"student02", "student123", "Sinh viên 2"},
                {"teacher01", "teacher123", "Giảng viên"},
                {"admin", "admin123", "Quản trị viên"}
        };

        for (String[] p : presets) {
            JButton btnP = new JButton(p[2]);
            btnP.setFont(Theme.FONT_SMALL);
            btnP.setForeground(Theme.TEXT_GOLD);
            btnP.setBackground(Theme.BG_CARD);
            btnP.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
            btnP.setFocusPainted(false);
            btnP.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnP.addActionListener(e -> {
                txtLoginUser.setText(p[0]);
                txtLoginPass.setText(p[1]);
                doLogin();
            });
            pnlPresets.add(btnP);
        }

        // Host / Port Bar
        JPanel pnlConn = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pnlConn.setOpaque(false);

        JLabel lblH = new JLabel("Host:");
        lblH.setFont(Theme.FONT_SMALL);
        lblH.setForeground(Theme.TEXT_MUTED);
        txtHost = new ModernTextField("127.0.0.1");
        txtHost.setText("127.0.0.1");
        txtHost.setPreferredSize(new Dimension(100, 28));
        txtHost.setFont(Theme.FONT_SMALL);

        JLabel lblP = new JLabel("Port:");
        lblP.setFont(Theme.FONT_SMALL);
        lblP.setForeground(Theme.TEXT_MUTED);
        txtPort = new ModernTextField("8888");
        txtPort.setText("8888");
        txtPort.setPreferredSize(new Dimension(60, 28));
        txtPort.setFont(Theme.FONT_SMALL);

        lblConnStatus = new JLabel("Sẵn sàng kết nối");
        lblConnStatus.setFont(Theme.FONT_BADGE);
        lblConnStatus.setForeground(Theme.SUCCESS);

        pnlConn.add(lblH);
        pnlConn.add(txtHost);
        pnlConn.add(lblP);
        pnlConn.add(txtPort);
        pnlConn.add(lblConnStatus);

        pnlBottom.add(pnlPresets, BorderLayout.NORTH);
        pnlBottom.add(pnlConn, BorderLayout.SOUTH);

        rootCard.add(pnlBottom, BorderLayout.SOUTH);
        add(rootCard, gbc);
    }

    private JPanel createLoginPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        JLabel lblU = new JLabel("Tên đăng nhập (Username):");
        lblU.setFont(Theme.FONT_BODY_BOLD);
        lblU.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblU, gbc);

        gbc.gridy++;
        txtLoginUser = new ModernTextField("Nhập tên đăng nhập hoặc mã sinh viên...");
        txtLoginUser.setText("student01");
        pnl.add(txtLoginUser, gbc);

        gbc.gridy++;
        JLabel lblP = new JLabel("Mật khẩu (Password):");
        lblP.setFont(Theme.FONT_BODY_BOLD);
        lblP.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblP, gbc);

        gbc.gridy++;
        txtLoginPass = new ModernTextField.PasswordField("Nhập mật khẩu...");
        txtLoginPass.setText("student123");
        pnl.add(txtLoginPass, gbc);

        gbc.gridy++;
        lblLoginError = new JLabel(" ", SwingConstants.CENTER);
        lblLoginError.setFont(Theme.FONT_SMALL);
        lblLoginError.setForeground(Theme.DANGER);
        pnl.add(lblLoginError, gbc);

        gbc.gridy++;
        btnLogin = new ModernButton("Đăng nhập Hệ thống", ModernButton.Style.HUST);
        btnLogin.setPreferredSize(new Dimension(200, 42));
        btnLogin.addActionListener(e -> doLogin());
        pnl.add(btnLogin, gbc);

        // Enter key listeners
        txtLoginUser.addActionListener(e -> doLogin());
        txtLoginPass.addActionListener(e -> doLogin());

        return pnl;
    }

    private JPanel createRegisterPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(12, 16, 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        JLabel lblU = new JLabel("Tên tài khoản (Username):");
        lblU.setFont(Theme.FONT_BODY_BOLD);
        lblU.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblU, gbc);

        gbc.gridy++;
        txtRegUser = new ModernTextField("Chọn tên đăng nhập...");
        pnl.add(txtRegUser, gbc);

        gbc.gridy++;
        JLabel lblP = new JLabel("Mật khẩu (Password):");
        lblP.setFont(Theme.FONT_BODY_BOLD);
        lblP.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblP, gbc);

        gbc.gridy++;
        txtRegPass = new ModernTextField.PasswordField("Chọn mật khẩu...");
        pnl.add(txtRegPass, gbc);

        gbc.gridy++;
        JLabel lblN = new JLabel("Họ và tên sinh viên (Full Name):");
        lblN.setFont(Theme.FONT_BODY_BOLD);
        lblN.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblN, gbc);

        gbc.gridy++;
        txtRegName = new ModernTextField("Ví dụ: Nguyễn Văn An");
        pnl.add(txtRegName, gbc);

        gbc.gridy++;
        JLabel lblE = new JLabel("Email HUST:");
        lblE.setFont(Theme.FONT_BODY_BOLD);
        lblE.setForeground(Theme.TEXT_SECONDARY);
        pnl.add(lblE, gbc);

        gbc.gridy++;
        txtRegEmail = new ModernTextField("an.nv210001@sis.hust.edu.vn");
        pnl.add(txtRegEmail, gbc);

        gbc.gridy++;
        lblRegStatus = new JLabel(" ", SwingConstants.CENTER);
        lblRegStatus.setFont(Theme.FONT_SMALL);
        lblRegStatus.setForeground(Theme.DANGER);
        pnl.add(lblRegStatus, gbc);

        gbc.gridy++;
        btnRegister = new ModernButton("Tạo Tài khoản Mới", ModernButton.Style.HUST_GOLD);
        btnRegister.setPreferredSize(new Dimension(200, 40));
        btnRegister.addActionListener(e -> doRegister());
        pnl.add(btnRegister, gbc);

        return pnl;
    }

    private void ensureConnected() throws Exception {
        if (!client.isConnected()) {
            String host = txtHost.getText().trim();
            int port = Integer.parseInt(txtPort.getText().trim());
            lblConnStatus.setText("Đang kết nối...");
            lblConnStatus.setForeground(Theme.WARNING);
            client.connect(host, port);
            lblConnStatus.setText("Đã kết nối");
            lblConnStatus.setForeground(Theme.SUCCESS);
        }
    }

    private void doLogin() {
        String username = txtLoginUser.getText().trim();
        String password = new String(txtLoginPass.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblLoginError.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        lblLoginError.setForeground(Theme.TEXT_SECONDARY);
        lblLoginError.setText("Đang xác thực thông tin với máy chủ HUST...");
        btnLogin.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Packet resPacket;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    ensureConnected();
                    LoginRequest req = new LoginRequest(username, password);
                    Packet p = Packet.ok(OpCodes.LOGIN_REQ, req);
                    resPacket = client.sendRequest(p, OpCodes.LOGIN_RES).get();
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                if (error != null) {
                    lblLoginError.setForeground(Theme.DANGER);
                    lblLoginError.setText("Kết nối thất bại: " + error.getMessage());
                    lblConnStatus.setText("Mất kết nối");
                    lblConnStatus.setForeground(Theme.DANGER);
                    return;
                }

                if (resPacket != null && resPacket.isSuccess()) {
                    LoginResponse res = resPacket.getPayloadAs(LoginResponse.class);
                    if (res != null && res.isSuccess()) {
                        client.setAuthToken(res.getToken());
                        lblLoginError.setText("");
                        app.onLoginSuccess(res.toUserProfileDTO());
                    } else {
                        lblLoginError.setForeground(Theme.DANGER);
                        lblLoginError.setText(res != null ? res.getMessage() : "Đăng nhập thất bại");
                    }
                } else {
                    lblLoginError.setForeground(Theme.DANGER);
                    lblLoginError.setText(resPacket != null ? resPacket.getPayloadAsString() : "Sai tên đăng nhập hoặc mật khẩu");
                }
            }
        };
        worker.execute();
    }

    private void doRegister() {
        String username = txtRegUser.getText().trim();
        String password = new String(txtRegPass.getPassword()).trim();
        String name = txtRegName.getText().trim();
        String email = txtRegEmail.getText().trim();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
            lblRegStatus.setForeground(Theme.DANGER);
            lblRegStatus.setText("Vui lòng điền đủ tên tài khoản, mật khẩu và họ tên!");
            return;
        }

        lblRegStatus.setForeground(Theme.TEXT_SECONDARY);
        lblRegStatus.setText("Đang tạo tài khoản trên hệ thống...");
        btnRegister.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Packet resPacket;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    ensureConnected();
                    RegisterRequest req = new RegisterRequest(username, password, name, email);
                    Packet p = Packet.ok(OpCodes.REGISTER_REQ, req);
                    resPacket = client.sendRequest(p, OpCodes.REGISTER_RES).get();
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                if (error != null) {
                    lblRegStatus.setForeground(Theme.DANGER);
                    lblRegStatus.setText("Lỗi: " + error.getMessage());
                    return;
                }

                if (resPacket != null && resPacket.isSuccess()) {
                    RegisterResponse res = resPacket.getPayloadAs(RegisterResponse.class);
                    lblRegStatus.setForeground(Theme.SUCCESS);
                    lblRegStatus.setText("Tạo tài khoản thành công! Hãy chuyển sang Đăng nhập.");
                    txtLoginUser.setText(username);
                    txtLoginPass.setText(password);
                } else {
                    lblRegStatus.setForeground(Theme.DANGER);
                    lblRegStatus.setText(resPacket != null ? resPacket.getPayloadAsString() : "Đăng ký không thành công");
                }
            }
        };
        worker.execute();
    }
}
