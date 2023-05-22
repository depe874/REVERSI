
//完成版
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Server {
	private int port;
	private List<ClientUnit> clientList;
	private final long seed = System.currentTimeMillis();
	private Socket socket;
	private Object obj = new Object();

	private class ClientUnit {
		private Player player, opponent;
		private boolean state = true;
		private boolean isOppReady = false;

		private PrintWriter out;
		private String handiExistRequest;
		private String handilevelRequest[] = new String[4]; // 4回分のハンデの要求の保持用配列
		private String handiapprRequest[] = new String[4];// 4回分のハンデの承認、拒否の保持用配列

		ClientUnit(Socket socket, String initName) {
			try {
				player = new Player();
				opponent = new Player();
				player.setName(initName);
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void setName(String name) {
			player.setName(name);
		}
	}

	public Server(int port) {
		this.port = port;
		clientList = new ArrayList<>(); // プレイヤリストを用意
	}

	public void acceptClient() { // クライアントの接続(サーバの起動)
		try {
			System.out.println("サーバが起動しました．");
			ServerSocket ss = new ServerSocket(port);
			while (true) {
				socket = ss.accept();
				System.out.println("クライアントと接続しました．"); // テスト用出力
				String initName = getRandomName(16);
				ClientUnit t = new ClientUnit(socket, initName);
				clientList.add(t);
				new Receiver(socket, initName).start();
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public class Receiver extends Thread {
		private BufferedReader br;
		private String myName, oppName = null;
		private int handiExist; // ハンデの有無を保持
		private int requestNo;

		Receiver(Socket socket, String initName) {
			try {
				br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				myName = initName;
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}

		public void run() {
			try {
				int count = 0; // ハンデの段階の折衝回数のカウント
				int myRequest = 5; // 自分のハンデの段階の要求を保持
				int enemyRequest = 5; // 相手のハンデの段階の要求を保持
				int confirmHandi = 5; // 確定したハンデの段階

				while (true) {
					String inputLine = br.readLine();
					if (inputLine != null) {
						String header, body = null;
						if (inputLine.indexOf(':') == -1)
							header = inputLine;
						else {
							header = inputLine.substring(0, inputLine.indexOf(':'));
							body = inputLine.substring(header.length() + 1);
						}

						System.out.println("受信:「" + inputLine + "」");

						switch (header) {
						case "name": // ログイン、プレイヤリスト送信
							ClientUnit t = getByName(myName);
							if (checkName(body)) {
								send(myName, "nameCorrect:" + body);
								getByName(myName).setName(body);
								myName = body;
								for (var x : clientList) {
									if (x.state) {
										x.out.println("addPlayer:" + body); // 全員に新ログイン者を送る
										t.out.println("addPlayer:" + x.player.getName()); // 新ログイン者にリストを送る
									}
								}
							} else {
								send(myName, "nameError:");
							}
							break;

						case "challengeto": // 対戦申し込み
							oppName = body;
							getByName(myName).opponent.setName(body);
							send(oppName, "challenged:" + myName);
							for (var x : clientList) {
								x.out.println("changeState:" + myName + ":false");
								x.out.println("changeState:" + oppName + ":false");
							}
							requestNo = 0;
							break;

						case "cancelto": // 対戦申し込みのキャンセル
							send(body, "cancelled:");
							for (var x : clientList) {
								x.out.println("changeState:" + myName + ":true");
								x.out.println("changeState:" + oppName + ":true");
							}
							break;

						case "accept":
							oppName = body;
							getByName(myName).opponent.setName(body);
							send(body, "accepted:");
							for (var x : clientList) {
								x.out.println("removePlayer:" + myName);
								x.out.println("removePlayer:" + oppName);
							}
							getByName(myName).state = false;
							getByName(oppName).state = false;
							requestNo = 1;
							break;

						case "reject":
							oppName = body;
							send(oppName, "rejected:");
							for (var x : clientList) {
								x.out.println("changeState:" + myName + ":true");
								x.out.println("changeState:" + oppName + ":true");
							}
							break;

						case "hdcpExist":
							getByName(myName).handiExistRequest = body;
							getByName(oppName).isOppReady = true;
							synchronized (obj) {
								while (!getByName(myName).isOppReady) {
									try {
										obj.wait();
										System.out.println("notified");
									} catch (Exception e) {
									}
								}
								obj.notifyAll();
							}
							handiExist = calcHandi1(Boolean.valueOf(body),
									Boolean.valueOf(getByName(oppName).handiExistRequest));
							if (handiExist == 0) {
								handiExist = requestNo; // 申し込み順による先手後手
							}
							send(myName, "hdcp1Res1:" + handiExist);
							getByName(myName).isOppReady = false;
							break;

						case "hdcp2Hope":
							myRequest = Integer.parseInt(body);
							getByName(myName).handilevelRequest[count] = body;
							getByName(oppName).isOppReady = true;
							synchronized (obj) {
								while (!getByName(myName).isOppReady) {
									try {
										obj.wait();
									} catch (Exception e) {
									}
								}
								obj.notifyAll();
							}
							enemyRequest = Integer.parseInt(getByName(oppName).handilevelRequest[count]);
							confirmHandi = levelHandigap1(myRequest, enemyRequest);
							send(myName, "hdcp2Res1:" + confirmHandi);
							getByName(myName).isOppReady = false;
							break;

						case "hdcp2Responce":
							getByName(myName).handiapprRequest[count] = body; // 相手にも要求を見えるようにする
							getByName(oppName).isOppReady = true;
							synchronized (obj) {
								while (!getByName(myName).isOppReady) {
									try {
										obj.wait();
									} catch (Exception e) {
									}
								}
								obj.notifyAll();
							}
							confirmHandi = levelHandigap2(myRequest, enemyRequest,
									Boolean.valueOf(getByName(myName).handiapprRequest[count]),
									Boolean.valueOf(getByName(oppName).handiapprRequest[count]),
									count);
							send(myName, "hdcp2Res2:" + confirmHandi);
							count++;// 折衝の回数をカウント
							getByName(myName).isOppReady = false;
							break;

						case "resign":
						case "pass":
						case "put":
							send(oppName, inputLine); // もう一方に転送する
							break;
						}
					}
				}

			} catch (IOException e) { // 接続が切れたとき
				System.out.println("プレイヤ " + myName + "との接続が切れました．");
				String s = getByName(myName).opponent.getName();
				if (s != null && getByName(s) != null) {
					send(s, "disconnected");
				}
				clientList.remove(getByName(myName));
				for (var x : clientList) {
					x.out.println("removePlayer:" + myName);
				}
			}
		}

		private boolean checkName(String s) {
			for (var x : clientList) {
				if (x.player.getName().equals(s)) {
					return false;
				}
			}
			return true;
		}

		private void send(String to, String msg) {
			System.out.println("送信:「" + to + "」「" + msg + "」");
			getByName(to).out.println(msg);
			getByName(to).out.flush();
		}

		private ClientUnit getByName(String s) {
			for (var x : clientList) {
				if (x.player.getName().equals(s)) {
					return x;
				}
			}
			return null;
		}

	}

	// メソッド

	private String getRandomName(int len) {
		byte[] bytearray;
		String mystring;
		StringBuffer thebuffer;

		bytearray = new byte[256];
		new Random().nextBytes(bytearray);

		mystring = new String(bytearray, Charset.forName("UTF-8"));
		thebuffer = new StringBuffer();

		for (int m = 0; m < mystring.length(); m++) {
			char n = mystring.charAt(m);
			if (((n >= 'A' && n <= 'Z') || (n >= '0' && n <= '9')) && (len > 0)) {
				thebuffer.append(n);
				len--;
			}
		}

		return thebuffer.toString();
	}

	public int calcHandi1(boolean c1, boolean c2) { // ハンデの有無についての折衝
		int e_handi;

		if (c1 && !c2) {
			e_handi = 2; // 自分にハンデあり
		} else if (!c1 && c2) {
			e_handi = 3; // 相手にハンデあり
		} else {
			e_handi = 0; // ハンデなし
		}

		return e_handi;
	}

	public int levelHandigap1(int c1, int c2) { // ハンデの段階の要求についての折衝

		int h_level;

		if (c1 == c2) {
			h_level = c1; // 要求が一致
		} else {
			h_level = 5 + c2; // 5＋相手の要求の段階
		}

		return h_level;
	}

	public int levelHandigap2(int c1, int c2, boolean consent1, boolean consent2, int count) { // 相手の要求への承諾についての折衝

		int h_level;

		if (consent1 && !consent2) { // 1が承諾
			h_level = c2;
		} else if (!consent1 && consent2) { // 2が承諾
			h_level = c1;
		} else if (consent1 && consent2) { // ともに承諾

			int smallNum, bigNum;
			if (c1 > c2) {
				smallNum = c2;
				bigNum = c1;
			} else {
				smallNum = c1;
				bigNum = c2;
			}

			Random rand = new Random(seed); // ランダムで決定
			int random = rand.nextInt(1) + 1;
			if (random == 1) {
				h_level = bigNum;
			} else {
				h_level = smallNum;
			}
		} else { // 二人とも拒否
			if (count > 2) { // 4回繰り返して決まらなければ
				int smallNum, bigNum;
				if (c1 > c2) {
					smallNum = c2;
					bigNum = c1;
				} else {
					smallNum = c1;
					bigNum = c2;
				}
				Random rand = new Random(seed); // ランダムで決定
				int random = rand.nextInt(1) + 1;
				if (random == 1) {
					h_level = bigNum;
				} else {
					h_level = smallNum;
				}
			} else {
				// 未確定
				h_level = 5 + count; // 5 + 班での折衝の残り回数
			}
		}

		return h_level;
	}

	public static void main(String[] args) {
		Server server = new Server(10000);
		server.acceptClient();

	}
}