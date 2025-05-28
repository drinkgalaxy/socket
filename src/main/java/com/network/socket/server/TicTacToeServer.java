package com.network.socket.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TicTacToeServer {
	private final int port;
	private ServerSocket serverSocket;
	private Socket socketX, socketO;
	private PrintWriter outX, outO;
	private BufferedReader inX, inO;

	private char[][] board;
	private char currentPlayer;
	// 플래그: 재시작 거부/종료 처리 중에는 추가 DISCONNECT 메시지 억제
	private volatile boolean terminationInProgress;
	private int restartCount;
	private boolean awaitingRestart;

	public static void main(String[] args) {
		new TicTacToeServer(5000).start();
	}

	public TicTacToeServer(int port) {
		this.port = port;
	}

	public void start() {
		while (true) {
			resetState();
			try {
				serverSocket = new ServerSocket(port);
				System.out.println("서버 대기 중: 포트 " + port);

				socketX = serverSocket.accept(); setupPlayer(socketX, 'X');
				System.out.println("플레이어 X 연결됨.");
				socketO = serverSocket.accept(); setupPlayer(socketO, 'O');
				System.out.println("플레이어 O 연결됨.");

				serverSocket.close();
				broadcast("MESSAGE:두 명 모두 접속했습니다.");
				sendTurn();

				Thread tX = new Thread(() -> handleClient(inX, 'X'));
				Thread tO = new Thread(() -> handleClient(inO, 'O'));
				tX.start(); tO.start();
				tX.join(); tO.join();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				closeConnections();
				System.out.println("게임 종료. 새로운 연결 대기 중...");
			}
		}
	}

	private void resetState() {
		terminationInProgress = false;
		board = new char[3][3];
		currentPlayer = 'X';
		restartCount = 0;
		awaitingRestart = false;
	}

	private void setupPlayer(Socket socket, char mark) throws IOException {
		if (mark == 'X') {
			outX = new PrintWriter(socket.getOutputStream(), true);
			inX  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outX.println("ASSIGN:X");
		} else {
			outO = new PrintWriter(socket.getOutputStream(), true);
			inO  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outO.println("ASSIGN:O");
		}
	}

	private void handleClient(BufferedReader in, char player) {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("MOVE:")) {
					handleMove(line, player);
				} else if (line.equals("RESTART_REQUEST")) {
					handleRestart();
				} else if (line.equals("RESTART_DECLINE")) {
					// 재시작 거부 처리 시작
					terminationInProgress = true;
					// 재시작 거부 처리: 거부한 주체에게 GAME_TERMINATED, 요청자에게 OPPONENT_DISCONNECTED 전송
					PrintWriter decliner = getOut(player);
					PrintWriter requester = getOtherOut(player);
					decliner.println("GAME_TERMINATED");
					requester.println("OPPONENT_DISCONNECTED");
					closeConnections();
					return;
				}
			}
		} catch (IOException e) {
			// 소켓 끊김: 상대방 강제 종료, 단 이미 종료 플로우라면 생략
			if (!terminationInProgress) {
				getOtherOut(player).println("OPPONENT_DISCONNECTED");
			}
		}
	}

	private synchronized void handleMove(String line, char player) {
		if (awaitingRestart) return;
		String[] p = line.substring(5).split(",");
		int r = Integer.parseInt(p[0]), c = Integer.parseInt(p[1]);
		if (player != currentPlayer) {
			getOut(player).println("MESSAGE:상대방의 차례입니다.");
		} else if (!isValid(r, c)) {
			getOut(player).println("MESSAGE:잘못된 위치입니다.");
		} else {
			board[r][c] = player;
			getOut(player).println("VALID_MOVE:" + r + "," + c);
			getOtherOut(player).println("OPPONENT_MOVED:" + r + "," + c);
			if (checkWin(player)) {
				getOut(player).println("VICTORY");
				getOtherOut(player).println("DEFEAT");
				promptRestart();
			} else if (checkDraw()) {
				broadcast("DRAW");
				promptRestart();
			} else {
				currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
				sendTurn();
			}
		}
	}

	private synchronized void handleRestart() {
		restartCount++;
		if (restartCount >= 2) {
			resetState();
			broadcast("MESSAGE:게임을 재시작합니다.");
			sendTurn();
		}
	}

	private void promptRestart() {
		awaitingRestart = true;
		broadcast("GAME_OVER");
	}

	private void sendTurn() {
		getOut(currentPlayer).println("YOUR_TURN");
	}

	private void broadcast(String msg) {
		outX.println(msg);
		outO.println(msg);
	}

	private PrintWriter getOut(char p) {
		return (p == 'X') ? outX : outO;
	}
	private PrintWriter getOtherOut(char p) {
		return (p == 'X') ? outO : outX;
	}

	private boolean isValid(int r, int c) {
		return r >= 0 && r < 3 && c >= 0 && c < 3 && board[r][c] == '\0';
	}

	private boolean checkWin(char p) {
		for (int i = 0; i < 3; i++) {
			if ((board[i][0] == p && board[i][1] == p && board[i][2] == p) ||
				(board[0][i] == p && board[1][i] == p && board[2][i] == p)) return true;
		}
		if ((board[0][0] == p && board[1][1] == p && board[2][2] == p) ||
			(board[0][2] == p && board[1][1] == p && board[2][0] == p)) return true;
		return false;
	}

	private boolean checkDraw() {
		for (char[] row : board)
			for (char c : row)
				if (c == '\0') return false;
		return true;
	}

	private void closeConnections() {
		try {
			if (socketX != null) socketX.close();
			if (socketO != null) socketO.close();
		} catch (IOException ignored) {}
	}
}



