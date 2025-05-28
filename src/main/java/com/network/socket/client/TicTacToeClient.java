package com.network.socket.client;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.*;

public class TicTacToeClient extends JFrame {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private char myMark;
	private boolean myTurn;
	private JButton[][] buttons = new JButton[3][3];
	private JLabel statusLabel = new JLabel("서버에 연결중...");

	public TicTacToeClient(String addr) throws IOException {
		socket = new Socket(addr, 5000);
		in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out    = new PrintWriter(socket.getOutputStream(), true);

		initGUI();
		listenServer();
	}

	private void initGUI() {
		setTitle("틱택토 TCP 게임"); setSize(300,350);
		setLayout(new BorderLayout());
		JPanel panel = new JPanel(new GridLayout(3,3));
		Font font = new Font("Arial", Font.BOLD, 60);

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				JButton b = new JButton("");
				b.setFont(font);
				b.setFocusPainted(false);
				final int r = i, c = j;
				b.addActionListener(e -> {
					if (!myTurn) {
						JOptionPane.showMessageDialog(this, "상대방의 차례입니다.");
					} else if (!b.getText().equals("")) {
						// do nothing
					} else {
						out.println("MOVE:" + r + "," + c);
					}
				});
				buttons[i][j] = b;
				panel.add(b);
			}
		}
		add(panel, BorderLayout.CENTER);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(statusLabel, BorderLayout.SOUTH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void listenServer() {
		new Thread(() -> {
			try {
				String line;
				while ((line = in.readLine()) != null) {
					String cmd = line.split(":", 2)[0];
					switch (cmd) {
						case "ASSIGN":
							myMark = line.charAt(line.indexOf(":")+1);
							statusLabel.setText("당신의 말은 " + myMark +
								" 입니다. 상대방의 접속을 기다리는 중...");
							break;
						case "YOUR_TURN":
							myTurn = true;
							statusLabel.setText("당신의 차례 (" + myMark + ")");
							break;
						case "VALID_MOVE":
							applyMove(line, myMark);
							myTurn = false;
							statusLabel.setText("완료, 상대방 차례");
							break;
						case "OPPONENT_MOVED":
							applyMove(line, myMark == 'X' ? 'O' : 'X');
							statusLabel.setText("당신의 차례 (" + myMark + ")");
							break;
						case "MESSAGE":
							statusLabel.setText(line.substring(8));
							break;
						case "GAME_OVER":
							int sel = JOptionPane.showConfirmDialog(this,
								"게임 종료! 다시 시작할까요?", "재시작",
								JOptionPane.YES_NO_OPTION);
							if (sel == JOptionPane.YES_OPTION) {
								clearBoard();
								statusLabel.setText("상대방의 요청을 기다리는 중...");
								out.println("RESTART_REQUEST");
							} else {
								out.println("RESTART_DECLINE");
								showEnd();
								return;
							}
							break;
						case "VICTORY":
							JOptionPane.showMessageDialog(this, "승리!");
							break;
						case "DEFEAT":
							JOptionPane.showMessageDialog(this, "패배...");
							break;
						case "DRAW":
							JOptionPane.showMessageDialog(this, "무승부");
							break;
						case "GAME_TERMINATED":
							showEnd();
							dispose();
							return;
						case "OPPONENT_DISCONNECTED":
							JOptionPane.showMessageDialog(this, "상대방이 게임을 종료했습니다.");
							try { socket.close(); } catch (IOException ignored) {}
							dispose();
							return;
					}
				}
			} catch (IOException e) {
				showEnd();
			}
		}).start();
	}

	private void applyMove(String line, char mark) {
		String[] p = line.substring(line.indexOf(":") + 1).split(",");
		int r = Integer.parseInt(p[0]), c = Integer.parseInt(p[1]);
		buttons[r][c].setText(String.valueOf(mark));
	}

	private void clearBoard() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				buttons[i][j].setText("");
		myTurn = false;
	}

	private void showEnd() {
		JOptionPane.showMessageDialog(this, "게임이 종료되었습니다.");
		try { socket.close(); } catch (IOException ignored) {}
		dispose();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				new TicTacToeClient(args.length > 0 ? args[0] : "localhost");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "접속 실패: " + e.getMessage());
				System.exit(0);
			}
		});
	}
}







