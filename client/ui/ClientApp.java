package client.ui;

import client.net.NetworkClient;
import client.ui.screens.*;
import shared.dtos.ExamInitDataDTO;
import shared.dtos.PracticeResponseDTO;
import shared.dtos.RoomInfoDTO;
import shared.dtos.UserProfileDTO;

import javax.swing.*;
import java.awt.*;

/**
 * Main Client GUI Application Window.
 * Houses CardLayout navigation, global network client instance,
 * and page transition routing.
 */
public class ClientApp extends JFrame {

    private final NetworkClient client;

    private final CardLayout cardLayout;
    private final JPanel rootPanel;

    // Screens
    private LoginScreen loginScreen;
    private DashboardScreen dashboardScreen;
    private PracticeSetupScreen practiceSetupScreen;
    private RoomLobbyListScreen roomLobbyListScreen;
    private RoomWaitingScreen roomWaitingScreen;
    private ExamScreen examScreen;
    private HistoryStatsScreen historyStatsScreen;
    private LeaderboardScreen leaderboardScreen;
    private AdminQuestionsScreen adminQuestionsScreen;

    public ClientApp() {
        this.client = new NetworkClient();
        this.cardLayout = new CardLayout();
        this.rootPanel = new JPanel(cardLayout);

        initWindow();
        initScreens();
        showScreen("LOGIN");
    }

    private void initWindow() {
        setTitle("Online Examination System - PTIT Computer Network");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 780);
        setMinimumSize(new Dimension(950, 680));
        setLocationRelativeTo(null);

        // Dark background on frame root
        getContentPane().setBackground(Theme.BG_DARK);
        rootPanel.setBackground(Theme.BG_DARK);
        add(rootPanel, BorderLayout.CENTER);
    }

    private void initScreens() {
        loginScreen = new LoginScreen(this);
        dashboardScreen = new DashboardScreen(this);
        practiceSetupScreen = new PracticeSetupScreen(this);
        roomLobbyListScreen = new RoomLobbyListScreen(this);
        roomWaitingScreen = new RoomWaitingScreen(this);
        examScreen = new ExamScreen(this);
        historyStatsScreen = new HistoryStatsScreen(this);
        leaderboardScreen = new LeaderboardScreen(this);
        adminQuestionsScreen = new AdminQuestionsScreen(this);

        rootPanel.add(loginScreen, "LOGIN");
        rootPanel.add(dashboardScreen, "DASHBOARD");
        rootPanel.add(practiceSetupScreen, "PRACTICE_SETUP");
        rootPanel.add(roomLobbyListScreen, "ROOM_LOBBY");
        rootPanel.add(roomWaitingScreen, "ROOM_WAITING");
        rootPanel.add(examScreen, "EXAM");
        rootPanel.add(historyStatsScreen, "HISTORY_STATS");
        rootPanel.add(leaderboardScreen, "LEADERBOARD");
        rootPanel.add(adminQuestionsScreen, "ADMIN_QUESTIONS");
    }

    public void showScreen(String screenName) {
        cardLayout.show(rootPanel, screenName);
    }

    public NetworkClient getClient() {
        return client;
    }

    // ==========================================
    // SCREEN ROUTING & WORKFLOW ACTIONS
    // ==========================================

    public void onLoginSuccess(UserProfileDTO user) {
        client.setCurrentUser(user);
        dashboardScreen.updateUserData(user);
        showScreen("DASHBOARD");
    }

    public void enterRoomLobby() {
        roomLobbyListScreen.loadRooms();
        showScreen("ROOM_LOBBY");
    }

    public void enterRoomWaitingScreen(String roomCode, RoomInfoDTO info, boolean isHost) {
        roomWaitingScreen.setupRoom(roomCode, info, isHost);
        showScreen("ROOM_WAITING");
    }

    public void startPracticeExam(PracticeResponseDTO practiceData) {
        examScreen.startPracticeExam(practiceData);
        showScreen("EXAM");
    }

    public void startMultiplayerExam(String roomCode, RoomInfoDTO info, ExamInitDataDTO examData, boolean isHost) {
        examScreen.startMultiplayerExam(roomCode, info, examData, isHost);
        showScreen("EXAM");
    }

    public void enterHistoryStats() {
        historyStatsScreen.loadData();
        showScreen("HISTORY_STATS");
    }

    public void enterLeaderboard() {
        leaderboardScreen.loadLeaderboard();
        showScreen("LEADERBOARD");
    }

    public void enterAdminQuestions() {
        adminQuestionsScreen.loadData();
        showScreen("ADMIN_QUESTIONS");
    }

    // ==========================================
    // APPLICATION LAUNCHER
    // ==========================================

    public static void main(String[] args) {
        // Enable anti-aliasing system wide in Swing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                // Try to set system look and feel or Nimbus
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}

            ClientApp app = new ClientApp();
            app.setVisible(true);
        });
    }
}
