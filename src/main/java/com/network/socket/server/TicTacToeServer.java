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
	private PrintWriter outX, outO; // X, O 두기
	private BufferedReader inX, inO;

	private char[][] board; // 게임판
	private char currentPlayer; // 현재 차례 플레이어 체크
	private volatile boolean terminationInProgress; // 재시작 플로우 여부 체크
	private int restartCount; // 재시작 요청 횟수 체크 (2가 되어야 재시작 할 수 있음 = 플레이어 1, 2 동의)
	private boolean awaitingRestart; // 재시작을 대기중인지 체크 (true 일 땐 move 요청 무시)

	public static void main(String[] args) { // ServerSocket 생성
		new TicTacToeServer(5000).start();
	}

	public TicTacToeServer(int port) {
		this.port = port;
	}

	public void start() {
		while (true) {
			resetState();
			try {
				serverSocket = new ServerSocket(port); // 서버 생성
				System.out.println("서버 생성 완료. 포트 "+port +"에서 대기 중");

				socketX = serverSocket.accept(); // client 접속 accept
				setupPlayer(socketX, 'X'); // 먼저 들어온 플레이어를 X
				System.out.println("플레이어 X 연결됨.");
				socketO = serverSocket.accept(); // client 접속 accept
				setupPlayer(socketO, 'O'); // 나중에 들어온 플레이어를 O
				System.out.println("플레이어 O 연결됨.");

				serverSocket.close(); // 두 명이 접속하면 다른 플레이어를 받지 않도록 close
				broadcast("MESSAGE:플레이어 모두 접속 완료. 게임을 시작합니다.");
				sendTurn();

				// 두 클라이언트의 스트림을 동시에 처리, 메시지 즉시 처리 의도
				Thread tX = new Thread(() -> handleClient(inX, 'X'));
				Thread tO = new Thread(() -> handleClient(inO, 'O'));
				tX.start();
				tO.start(); // 스레드 시작
				tX.join();
				tO.join(); // 스레드 대기 (즉 게임중)
				// 두 플레이어 중 하나가 종료되면 스레드가 join()을 풀어주고 루프의 finally 로 이동
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				closeConnections(); // 두 플레이어의 스레드 전부 종료
				System.out.println("게임 종료.");
			}
		}
	}

	// 게임판 초기화
	private void resetState() {
		terminationInProgress = false; // 재시작 여부 초기화
		board = new char[3][3]; // 틱택토 보드를 빈 상태로 초기화
		currentPlayer = 'X'; // 틱택토는 항상 X가 먼저 말을 두기 때문에 현재 플레이어를 X 로 초기화
		restartCount = 0; // 재시작 요청이 2가 되어야 다시 시작하는데 0으로 초기화
		awaitingRestart = false; // 재시작 대기 여부 초기화
	}

	// 플레이어 설정
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
				} else if (line.equals("RESTART_REQUEST")) { // 재시작 선택
					handleRestart();
				} else if (line.equals("RESTART_DECLINE")) { // 재시작 거부
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
			getOut(player).println("MESSAGE:잘못된 위치입니다."); // 게임판 초과하면 에러, 이미 놓은 곳에 놓으면 에러
		} else {
			board[r][c] = player;
			getOut(player).println("VALID_MOVE:" + r + "," + c);
			getOtherOut(player).println("OPPONENT_MOVED:" + r + "," + c);
			if (checkWin(player)) {
				getOut(player).println("VICTORY");
				getOtherOut(player).println("DEFEAT");
				promptRestart();
			} else if (checkDraw()) { // 무승부
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
		awaitingRestart = true; // restart 기다리는 상태로 활성화
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
		return r >= 0 && r < 3 && c >= 0 && c < 3 && board[r][c] == '\0'; // char 타입의 null
	}

	private boolean checkWin(char p) {
		for (int i = 0; i < 3; i++) { // 행과 열 승리 검사
			if ((board[i][0] == p && board[i][1] == p && board[i][2] == p) ||
				(board[0][i] == p && board[1][i] == p && board[2][i] == p)) return true;
		}

		// 대각선 승리 검사
		if ((board[0][0] == p && board[1][1] == p && board[2][2] == p) ||
			(board[0][2] == p && board[1][1] == p && board[2][0] == p)) return true;

		return false;
	}

	private boolean checkDraw() {
		for (char[] row : board)
			for (char c : row)
				if (c == '\0') return false;
		return true; // 무승부 상태 => 보드가 꽉 차서 더 둘 수 없음
	}

	private void closeConnections() { // 실행중인 소켓 종료하기
		try {
			if (socketX != null) socketX.close();
			if (socketO != null) socketO.close();
		} catch (IOException ignored) {} // 종료 시 발생하는 예외 무시
	}
}



