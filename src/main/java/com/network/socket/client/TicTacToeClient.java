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
	private char myMark; // 자신이 두는 기호 (X or O) 저장
	private boolean myTurn; // 자신의 차례 여부 표시
	private JButton[][] buttons = new JButton[3][3]; // 틱택토 판에 대응되는 버튼 배열, 사용자 클릭 시 움직임을 서버에 전송
	private JLabel statusLabel = new JLabel("서버에 연결중..."); // 창 하단에 메시지 표시

	public TicTacToeClient(String addr) throws IOException {
		socket = new Socket(addr, 5000); // 지정된 서버 주소와 포트 5000 으로 TCP 연결 시도
		in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 서버의 텍스트 읽어옴
		out = new PrintWriter(socket.getOutputStream(), true); // 서버로 문자열을 보냄

		initGUI();
		listenServer();

		// 서버 연결 및 초기화
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
					} else if (!b.getText().isEmpty()) {
						statusLabel.setText("이미 선택된 칸입니다.");
					} else {
						// 선택한 정보를 MOVE:행,열 형태로 서버에 전송함
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
		new Thread(() -> { // 서버에서 오는 메시지를 처리하는 로직은 별도의 스레드 안에서 수행
			try {
				String line;
				while ((line = in.readLine()) != null) {
					String command = line.split(":", 2)[0]; // 서버에서 메시지를 받아온다.
					switch (command) {
						case "ASSIGN":
							myMark = line.charAt(line.indexOf(":")+1);
							statusLabel.setText("당신의 말은 "+ myMark +
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
							try {
								socket.close();
							} catch (IOException ignored) {}
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







