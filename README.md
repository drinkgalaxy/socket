### 🎮 틱택토 게임 개발
네트워크 프로그래밍 전공 수업 발표 자료

---

### 📌 주제 설명  
TCP 소켓 기반으로 제작된 틱택토(Tic-Tac-Toe) 게임으로, 두 명의 사용자가 실시간으로 접속하여 턴을 주고받으며 게임을 진행할 수 있도록 설계되었습니다. GUI는 Java Swing을 이용해 구현했습니다.

<img src="https://github.com/user-attachments/assets/4cf712bb-d180-4c42-8522-f13145876712" width="500"/>

---

### 🛠️ 사용 기술  
**Java**, **Java ServerSocket**, **Socket**, **Java Swing**

---

### 🔗 프로젝트의 TCP 소켓 통신 구조  

<img src="https://github.com/user-attachments/assets/192a5468-a6a8-4a84-955e-31a1d70c936f" width="500"/>

---

### ⚙️ 주요 기능 동작 원리 설명  

#### 1) 클라이언트 접속 후 게임 시작  
두 명의 클라이언트가 서버에 접속하면 자동으로 게임이 시작됩니다.

<img src="https://github.com/user-attachments/assets/755d25a1-7634-404a-8e59-5581d082f3fd" width="500"/>

#### 2) 말의 위치 선택  
사용자가 자신의 턴에 셀을 클릭해 위치를 선택합니다.

<img src="https://github.com/user-attachments/assets/9863e7b9-8718-40f5-97cc-614d3938e12d" width="500"/>

#### 3) 말의 위치를 화면에 출력하고 다음 턴 이동  
선택한 위치는 화면에 표시되고 턴이 전환됩니다.

<img src="https://github.com/user-attachments/assets/996764f9-59ad-4df2-a1a9-18333de74026" width="500"/>

#### 4) 한 턴의 결과 화면  
각 턴 이후 현재 게임 판의 상태를 확인할 수 있습니다.

<img src="https://github.com/user-attachments/assets/3e7517e0-a45a-4731-94c6-ab0fef67573d" width="500"/>

---

### 🚫 에러 처리  

#### 1) 유효한 말인지 검증  
이미 선택된 위치에 말을 놓으려고 할 경우, 경고 메시지를 출력합니다.

<img src="https://github.com/user-attachments/assets/4059e66c-eb4e-4c8b-baba-d1faa550247a" width="500"/>

#### 2) 게임 도중 상대방의 연결이 끊어짐  
상대방 클라이언트 연결이 종료되면, 알림 창을 띄우고 게임을 종료합니다.

<img src="https://github.com/user-attachments/assets/cf448e18-6002-4830-92bb-85fad58fab1d" width="500"/>

#### 3) 게임 종료 후 재시작  
게임이 종료되면 재시작 여부를 묻고, "예" 선택 시 새 게임을 시작합니다.

<img src="https://github.com/user-attachments/assets/43fcc9df-1ee5-4078-981b-bc6ef37af93b" width="500"/>
<img src="https://github.com/user-attachments/assets/b44771d7-5713-4891-8cba-2907573249cc" width="500"/>
