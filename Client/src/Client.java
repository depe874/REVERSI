import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.Dialog.ModalityType;

import java.awt.event.*;
import java.awt.image.ImageProducer;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client extends JFrame implements ActionListener {
	public Othello game;
	private Player player = new Player(), opponent = new Player();
	private Client client;
	private ChallengedMenu cm;

	private PrintWriter out;
	private Receiver receiver;

	private HashMap<String, JLayeredPane> screens;
	private HashMap<String, Clip> audios;
	private JScrollPane playerListScrollbar;
	private JPanel plPanel;
	private JButton[][] buttonArray;
	private JButton resign, pass, volume, button1, button2, hdcp[];
	private ArrayList<JButton> buttons1, buttons2;
	private ArrayList<String> playerList;
	private JLabel title, turnLabel, hdcpLabel, board, bg, player1_count, player2_count, label1, label2;
	private ImageIcon bgIcon, titleIcon, blackIcon, whiteIcon, boardIcon, blankIcon, volumeIconOn, volumeIconOff,
			hdcpIcon[], gameIcon;
	private Image blankImg, bgImg, titleImg, blackImg, whiteImg, boardImg, volumeImgOn, volumeImgOff, hdcpImg[];

	private boolean volume_enabled = true, isLoggedin = false;
	private int row = 8, tmp, tmp2;
	private double size_r = 1;

	private static final int DISK = 200, TILE = 236, MARGIN = 80, HDCP_W = 1280, HDCP_H = 720,
			BOARD = 2000, FRAME = 136, VOL = 500, DEFW = 3840, DEFH = 2160,
			TITW = 1822, TITH = 806;
	private static final String[] s_hdcp = { "引き分け勝ち", "1子局", "2子局", "3子局", "4子局" },
			audioName = { "put", "cancel", "default", "notif" };

	public Client() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("リバーシ");

		init_audio();
		init_images();

		setVisible(true);
		Insets insets = getInsets();
		int init_h = (int) getToolkit().getScreenSize().getHeight() - 100;
		setSize((init_h - insets.top - insets.bottom) * 16 / 9, init_h);
		size_r = (double) init_h / (double) DEFH;

		playerList = new ArrayList<String>();
		client = this;
		screens = new HashMap<>();
		screens.put("title", new Title());

		for (var x : screens.values()) {
			x.setLayout(null);
		}

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent c) {
				size_r = (double) (getHeight() - insets.top - insets.bottom) / (double) DEFH;
				Insets insets = getInsets();
				int height = getHeight();
				setSize((height - insets.top - insets.bottom) * 16 / 9, getHeight());
				repaint();
			}
		});

		gotomenu("t");

		LoginMenu loginMenu = new LoginMenu(client, ModalityType.MODELESS);
		loginMenu.setVisible(true);
	}

	private void init_audio() {

		audios = new HashMap<>();
		for (int i = 0; i < audioName.length; i++) {
			audios.put(audioName[i], createClip(new File("snd\\" + audioName[i] + ".wav")));
		}
	}

	private Clip createClip(File path) {
		try (AudioInputStream ais = AudioSystem.getAudioInputStream(path)) {
			AudioFormat af = ais.getFormat();
			Clip c = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, af));
			c.open(ais);
			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void init_images() {

		bgIcon = new ImageIcon("img\\bg.png");
		bgImg = bgIcon.getImage();
		titleIcon = new ImageIcon("img\\title.png");
		titleImg = titleIcon.getImage();
		whiteIcon = new ImageIcon("img\\White.png");
		whiteImg = whiteIcon.getImage();
		blackIcon = new ImageIcon("img\\Black.png");
		blackImg = blackIcon.getImage();
		gameIcon = new ImageIcon("img\\icon.png");
		setIconImage(gameIcon.getImage());
		boardIcon = new ImageIcon("img\\Board.png");
		boardImg = boardIcon.getImage();
		blankIcon = new ImageIcon("img\\Blank.png");
		volumeIconOn = new ImageIcon("img\\volume_on.png");
		volumeImgOn = volumeIconOn.getImage();
		volumeIconOff = new ImageIcon("img\\volume_off.png");
		volumeImgOff = volumeIconOff.getImage();
		hdcpIcon = new ImageIcon[5];
		hdcpImg = new Image[5];
		for (int i = 0; i < hdcpIcon.length; i++) {
			hdcpIcon[i] = new ImageIcon("img\\" + i + ".png");
			hdcpImg[i] = hdcpIcon[i].getImage();
		}
	}

	private class Title extends JLayeredPane {
		Title() {
			board = new JLabel(boardIcon);
			add(board);
			setLayer(board, 100);

			bg = new JLabel(bgIcon);
			add(bg);
			setLayer(bg, 50);

			title = new JLabel(titleIcon);
			add(title);
			setLayer(title, 100);

			volume = new JButton(volumeIconOn);
			add(volume);
			volume.setBorderPainted(false);
			volume.setContentAreaFilled(false);
			volume.addActionListener(client);
			volume.setActionCommand("volume");
			setLayer(volume, 100);
		}

		public void repaint() {
			titleIcon.setImage(titleImg.getScaledInstance(
					autoScale(TITW), autoScale(TITH), Image.SCALE_SMOOTH));
			boardIcon.setImage(boardImg.getScaledInstance(
					autoScale(BOARD), autoScale(BOARD), Image.SCALE_FAST));
			bgIcon.setImage(bgImg.getScaledInstance(
					autoScale(DEFW), autoScale(DEFH), Image.SCALE_FAST));
			volumeIconOn.setImage(volumeImgOn.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));
			volumeIconOff.setImage(volumeImgOff.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));
			adjust(bg, 0, 0, DEFW, DEFH);
			adjust(board, MARGIN, MARGIN, BOARD, BOARD);
			adjust(title, BOARD, (DEFH - TITH) / 2, TITW, TITH);
			adjust(volume, BOARD + 1325, 1700, 500, 500);
			super.repaint();
		}
	}

	private class Match extends JLayeredPane {
		Match() {
			plPanel = new JPanel();
			plPanel.setLayout(null);
			plPanel.setOpaque(false);
			plPanel.setBorder(null);

			playerListScrollbar = new JScrollPane(plPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			playerListScrollbar.setViewportView(plPanel);
			playerListScrollbar.getViewport().setOpaque(false);
			playerListScrollbar.getViewport().setBorder(null);
			playerListScrollbar.setOpaque(false);
			playerListScrollbar.setBorder(null);
			playerListScrollbar.getVerticalScrollBar().setUnitIncrement(autoScale(25));

			add(playerListScrollbar);
			setLayer(playerListScrollbar, 200);

			board = new JLabel(boardIcon);
			add(board);
			setLayer(board, 100);

			bg = new JLabel(bgIcon);
			add(bg);
			setLayer(bg, 50);

			label1 = new JLabel("プレイヤリスト");
			add(label1);
			setLayer(label1, 100);
			label1.setForeground(Color.white);

			volume = new JButton(volumeIconOn);
			add(volume);
			volume.setBorderPainted(false);
			volume.setContentAreaFilled(false);
			volume.addActionListener(client);
			volume.setActionCommand("volume");
			setLayer(volume, 100);

			buttons1 = new ArrayList<>(playerList.size());
			buttons2 = new ArrayList<>(playerList.size());

		}

		public void repaint() {
			bgIcon.setImage(bgImg.getScaledInstance(
					autoScale(DEFW), autoScale(DEFH), Image.SCALE_FAST));
			blackIcon.setImage(blackImg.getScaledInstance(
					autoScale(DISK), autoScale(DISK), Image.SCALE_SMOOTH));
			whiteIcon.setImage(whiteImg.getScaledInstance(
					autoScale(DISK), autoScale(DISK), Image.SCALE_SMOOTH));
			boardIcon.setImage(boardImg.getScaledInstance(
					autoScale(BOARD), autoScale(BOARD), Image.SCALE_FAST));
			volumeIconOn.setImage(volumeImgOn.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));
			volumeIconOff.setImage(volumeImgOff.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));

			board.setIcon(boardIcon);
			adjust(board, MARGIN, MARGIN, BOARD, BOARD);

			adjust(label1, BOARD + 250, 100, 1000, 200);
			adjust(playerListScrollbar, BOARD + 200, 400, 1450, 1300);

			plPanel.setPreferredSize(new Dimension(autoScale(1450), autoScale(playerList.size() * 298)));
			plPanel.removeAll();

			repaintPlayerList();

			adjust(volume, BOARD + 1325, 1700, 500, 500);
			bg.setIcon(bgIcon);
			adjust(bg, 0, 0, DEFW, DEFH);

			label1.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(100)));
			super.repaint();
		}
	}

	private class Hdcp1 extends JLayeredPane {
		Hdcp1() {
			board = new JLabel(boardIcon);
			add(board);
			setLayer(board, 100);

			bg = new JLabel(bgIcon);
			add(bg);
			setLayer(bg, 50);

			add(label1);
			setLayer(label1, 100);
			String s = player.getColor() == true ? "先手（黒）" : "後手（白）";
			label1.setText("<html><body>あなたの対戦相手は<br />"
					+ opponent.getName()
					+ "<br />に決まりました。<br />あなたは" + s + "です。</body></html>");

			button1 = new JButton();
			button1.setText("ハンデを希望する");
			button1.setMargin(new Insets(0, 0, 0, 0));
			add(button1);
			setLayer(button1, 100);
			button1.addActionListener(client);
			button1.setActionCommand("hope_hdcp");
			button1.setBackground(Color.WHITE);

			button2 = new JButton("ハンデを希望しない");
			button2.setMargin(new Insets(0, 0, 0, 0));
			add(button2);
			setLayer(button2, 100);
			button2.addActionListener(client);
			button2.setActionCommand("hope_nohdcp");
			button2.setBackground(Color.WHITE);

			add(volume);
			setLayer(volume, 100);
		}

		public void repaint() {
			bgIcon.setImage(bgImg.getScaledInstance(
					autoScale(DEFW), autoScale(DEFH), Image.SCALE_FAST));
			boardIcon.setImage(boardImg.getScaledInstance(
					autoScale(BOARD), autoScale(BOARD), Image.SCALE_FAST));
			volumeIconOn.setImage(volumeImgOn.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));
			volumeIconOff.setImage(volumeImgOff.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));

			adjust(board, MARGIN, MARGIN, BOARD, BOARD);
			adjust(label1, BOARD + 250, 100, 1500, 600);
			adjust(button1, BOARD + 500, 900, 1000, 300);
			adjust(button2, BOARD + 500, 1300, 1000, 300);
			adjust(volume, BOARD + 1325, 1700, 500, 500);
			adjust(bg, 0, 0, DEFW, DEFH);

			label1.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(100)));

			super.repaint();
		}
	}

	private class Hdcp2 extends JLayeredPane {
		Hdcp2() {
			board = new JLabel(boardIcon);
			add(board);
			setLayer(board, 100);

			bg = new JLabel(bgIcon);
			add(bg);
			setLayer(bg, 50);

			hdcp = new JButton[5];
			for (int i = 0; i < hdcpIcon.length; i++) {
				hdcp[i] = new JButton(hdcpIcon[i]);
				adjust(hdcp[i], 50 + HDCP_W * (i % 2), 50 + HDCP_H * (i / 3), HDCP_W, HDCP_H);
				add(hdcp[i]);
				setLayer(hdcp[i], 100);
				hdcp[i].addActionListener(client);
				hdcp[i].setActionCommand("hdcp:" + i);
			}

			add(volume);
			setLayer(volume, 100);

			revalidate();
			repaint();
		}

		public void repaint() {
			bgIcon.setImage(bgImg.getScaledInstance(
					autoScale(DEFW), autoScale(DEFH), Image.SCALE_FAST));
			volumeIconOn.setImage(volumeImgOn.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));
			volumeIconOff.setImage(volumeImgOff.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));

			for (int i = 0; i < hdcpIcon.length; i++) {
				hdcpIcon[i].setImage(hdcpImg[i].getScaledInstance(
						autoScale(HDCP_W), autoScale(HDCP_H), Image.SCALE_SMOOTH));
				adjust(hdcp[i], HDCP_W * ((i + 1) % 2),
						(DEFH - 2 * HDCP_H) / 2 + HDCP_H * ((int) (((double) i - 0.5) / 2.0)),
						HDCP_W, HDCP_H);
			}
			adjust(hdcp[0], HDCP_W * 2, (int) ((DEFH - 2 * HDCP_H) / 2 + (double) HDCP_H * 0.5),
					HDCP_W, HDCP_H);
			adjust(bg, 0, 0, DEFW, DEFH);
			adjust(volume, BOARD + 1325, 1700, 500, 500);

			super.revalidate();
			super.repaint();
		}
	}

	private class Game extends JLayeredPane {
		Game() {
			board = new JLabel(boardIcon);
			add(board);
			setLayer(board, 100);

			bg = new JLabel(bgIcon);
			add(bg);
			setLayer(bg, 50);

			buttonArray = new JButton[row][row];
			for (int i = 0; i < row; i++) {
				for (int j = 0; j < row; j++) {
					if (game.grids.get(i).get(j).equals(1)) {
						buttonArray[i][j] = new JButton(blackIcon);
						buttonArray[i][j].setBorderPainted(false);
						buttonArray[i][j].setContentAreaFilled(false);
					} else if (game.grids.get(i).get(j).equals(2)) {
						buttonArray[i][j] = new JButton(whiteIcon);
						buttonArray[i][j].setBorderPainted(false);
						buttonArray[i][j].setContentAreaFilled(false);
					} else {
						buttonArray[i][j] = new JButton(blankIcon);
						buttonArray[i][j].setBorderPainted(false);
						buttonArray[i][j].setContentAreaFilled(false);
					}
					add(buttonArray[i][j]);
					setLayer(buttonArray[i][j], 200);

					buttonArray[i][j].addActionListener(client);
					buttonArray[i][j].setActionCommand("put:" + Integer.toString(j * 10 + i));
				}
			}

			resign = new JButton("投了");
			add(resign);
			resign.addActionListener(client);
			resign.setActionCommand("resign");
			resign.setBackground(Color.WHITE);
			setLayer(resign, 100);

			pass = new JButton("パス");
			add(pass);
			pass.addActionListener(client);
			pass.setActionCommand("pass");
			pass.setBackground(Color.WHITE);
			pass.setEnabled(false);
			pass.setBackground(Color.GRAY);
			setLayer(pass, 100);

			volume = new JButton(volumeIconOn);
			add(volume);
			volume.setBorderPainted(false);
			volume.setContentAreaFilled(false);
			volume.addActionListener(client);
			volume.setActionCommand("volume");
			setLayer(volume, 100);

			turnLabel = new JLabel();
			if (game.getTurn() == true) {
				turnLabel.setText("黒の番です");
			} else {
				turnLabel.setText("白の番です");
			}
			turnLabel.setForeground(Color.white);
			add(turnLabel);
			setLayer(turnLabel, 100);

			hdcpLabel = new JLabel("ハンデ：" + player.getHandi());
			add(hdcpLabel);
			hdcpLabel.setForeground(Color.white);
			setLayer(hdcpLabel, 100);

			label1 = new JLabel(player.getColor() == true ? player.getName() : opponent.getName());
			label1.setIcon(blackIcon);
			add(label1);
			label1.setForeground(Color.white);
			setLayer(label1, 100);

			label2 = new JLabel(player.getColor() == false ? player.getName() : opponent.getName());
			label2.setIcon(whiteIcon);
			add(label2);
			label2.setForeground(Color.white);
			setLayer(label2, 100);

			player1_count = new JLabel(game.bStone + "個");
			add(player1_count);
			player1_count.setForeground(Color.white);
			setLayer(player1_count, 100);

			player2_count = new JLabel(game.wStone + "個");
			add(player2_count);
			player2_count.setForeground(Color.white);
			setLayer(player2_count, 100);
		}

		public void repaint() {
			bgIcon.setImage(bgImg.getScaledInstance(
					autoScale(DEFW), autoScale(DEFH), Image.SCALE_FAST));
			blackIcon.setImage(blackImg.getScaledInstance(
					autoScale(DISK), autoScale(DISK), Image.SCALE_SMOOTH));
			whiteIcon.setImage(whiteImg.getScaledInstance(
					autoScale(DISK), autoScale(DISK), Image.SCALE_SMOOTH));
			boardIcon.setImage(boardImg.getScaledInstance(
					autoScale(BOARD), autoScale(BOARD), Image.SCALE_SMOOTH));
			volumeIconOn.setImage(volumeImgOn.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));
			volumeIconOff.setImage(volumeImgOff.getScaledInstance(
					autoScale(VOL), autoScale(VOL), Image.SCALE_SMOOTH));

			if (game.pass && player.getColor() == game.getTurn()) {
				pass.setEnabled(true);
				pass.setBackground(Color.WHITE);
			} else {
				pass.setEnabled(false);
				pass.setBackground(Color.GRAY);
			}

			if (game.getTurn() == true) {
				turnLabel.setText("黒の番です");
			} else {
				turnLabel.setText("白の番です");
			}

			player1_count.setText(game.bStone + "個");
			player2_count.setText(game.wStone + "個");

			for (int m = 0; m < row; m++) {
				for (int n = 0; n < row; n++) {
					if (game.grids.get(m).get(n).equals(1)) {
						buttonArray[n][m].setIcon(blackIcon);
					} else if (game.grids.get(m).get(n).equals(2)) {
						buttonArray[n][m].setIcon(whiteIcon);
					} else {
						buttonArray[n][m].setIcon(blankIcon);
					}
					adjust(buttonArray[m][n],
							m * TILE + FRAME, n * TILE + FRAME, TILE, TILE);
				}
			}

			adjust(board, MARGIN, MARGIN, BOARD, BOARD);
			adjust(bg, 0, 0, DEFW, DEFH);
			adjust(label1, BOARD + 200, 100, 2000, 250);
			adjust(label2, BOARD + 200, 300, 2000, 250);
			adjust(player1_count, BOARD + 1400, 100, 2000, 250);
			adjust(player2_count, BOARD + 1400, 300, 2000, 250);
			adjust(resign, BOARD + 250, 1400, 500, 250);
			adjust(pass, BOARD + 1050, 1400, 500, 250);
			adjust(turnLabel, BOARD + 250, 1800, 1000, 200);
			adjust(hdcpLabel, BOARD + 250, 700, 1000, 200);
			adjust(volume, BOARD + 1325, 1700, 500, 500);

			label1.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(120)));
			label2.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(120)));
			player1_count.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(120)));
			player2_count.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(120)));
			resign.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(100)));
			pass.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(100)));
			turnLabel.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(100)));
			hdcpLabel.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(100)));

			super.repaint();
		}
	}

	private void repaintPlayerList() {
		plPanel.removeAll();
		for (int i = 0; i < playerList.size(); i++) {
			buttons1.get(i).setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(50)));
			buttons2.get(i).setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(50)));
			JLayeredPane p = new JLayeredPane();
			p.setLayout(null);
			p.setSize(new Dimension(autoScale(1450), autoScale(300)));
			p.setOpaque(true);
			p.setBackground(Color.GRAY);
			p.setBorder(new LineBorder(Color.WHITE, 3));
			JLabel l = new JLabel(playerList.get(i));
			l.setForeground(Color.WHITE);
			l.setSize(autoScale(1200), autoScale(300));
			l.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(90)));
			p.setLayer(l, 3000);
			adjust(l, 50, 75, 1000, 150);
			adjust(buttons1.get(i), 900, 75, 220, 150);
			p.setLayer(buttons1.get(i), 400);
			adjust(buttons2.get(i), 1170, 75, 220, 150);
			p.setLayer(buttons2.get(i), 400);

			p.add(l);
			p.add(buttons1.get(i));
			p.add(buttons2.get(i));
			adjust(p, 0, 298 * i, 1450, 300);
			plPanel.add(p);
		}
		revalidate();
		repaint();
	}

	private void repaintGame() {
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < row; j++) {
				switch (game.grids.get(i).get(j)) {
				case 1:
					buttonArray[j][i].setIcon(blackIcon);
					break;

				case 2:
					buttonArray[j][i].setIcon(whiteIcon);
					break;

				case 0:
				case 3:
					buttonArray[j][i].setIcon(blankIcon);
					break;
				}
			}

		}
		player1_count.setText(game.bStone + "個");
		player2_count.setText(game.wStone + "個");
		String s = game.getTurn() ? "黒" : "白";
		turnLabel.setText(s + "の番です");

		if (game.pass && player.getColor() == game.getTurn()) {
			pass.setEnabled(true);
			pass.setBackground(Color.WHITE);
		} else {
			pass.setEnabled(false);
			pass.setBackground(Color.GRAY);
		}

		revalidate();
		repaint();
	}

	public void connectServer(String ipAddress, int port) { // サーバに接続
		Socket socket = null;
		try {
			socket = new Socket(ipAddress, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			receiver = new Receiver(socket);
			receiver.start();

		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(this, """
					ホストのIPアドレスが判定できません。
					ゲームを終了します。
					""");
			System.exit(-1);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, """
					サーバ接続時にエラーが発生しました。
					ゲームを終了します。
					""");
			System.exit(-1);
		}
		isLoggedin = true;
	}

	public void sendMessage(String msg) { // サーバにメッセージを送信
		out.println(msg);
		out.flush();
		System.out.println("送信「" + msg + "」");
	}

	void receiveMessage(String msg) { // メッセージの受信、ヘッダとボディから成る
		String header, body = null;
		if (msg.indexOf(':') == -1)
			header = msg;
		else {
			header = msg.substring(0, msg.indexOf(':'));
			body = msg.substring(header.length() + 1);
		}

		System.out.println("受信：「" + header + "」「" + body + "」");

		switch (header) {
		case "nameError":
			JOptionPane.showMessageDialog(this, """
					名前が重複しています。
					ログインし直してください。
					""");
			gotomenu("l");
			break;

		case "nameCorrect":
			addPlayerName(body);
			break;

		case "addPlayer":
			if (!body.equals(player.getName())) {
				playerList.add(body);
				tmp = playerList.size() - 1;
				JButton b1 = new JButton("申込"), b2 = new JButton("取消");
				b1.addActionListener(this);
				b1.setMargin(new Insets(0, 0, 0, 0));
				b1.setActionCommand("challengeto:" + body);
				b1.setBackground(Color.white);
				b1.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(50)));
				if (!player.getState()) {
					b1.setEnabled(false);
					b1.setBackground(Color.GRAY);
				}
				b2.addActionListener(this);
				b2.setMargin(new Insets(0, 0, 0, 0));
				b2.setActionCommand("cancelto:" + body);
				b2.setEnabled(false);
				b2.setBackground(Color.GRAY);
				b2.setFont(new Font("UD デジタル 教科書体 NK-B", Font.PLAIN, autoScale(50)));
				buttons1.add(b1);
				buttons2.add(b2);
				repaintPlayerList();
			}
			break;

		case "removePlayer":
			if (!body.equals(player.getName()) && playerList.contains(body)) {
				tmp = playerList.indexOf(body);
				playerList.remove(tmp);
				plPanel.remove(tmp);
				buttons1.remove(tmp);
				buttons2.remove(tmp);
				repaintPlayerList();
			}
			break;

		case "changeState": // 待機中:true
			header = body.substring(0, body.indexOf(':'));
			String tail = body.substring(header.length() + 1);
			if (!header.equals(player.getName())) {
				tmp = playerList.indexOf(header);
				buttons1.get(tmp).setEnabled(Boolean.valueOf(tail));
			}
			break;

		case "accepted":
			playSound("notif");
			JOptionPane.showMessageDialog(this, """
					申し込みが承諾されました。
					ハンデ設定に進みます。
					""");
			playSound();
			gotomenu("h1");
			break;

		case "rejected":
			JOptionPane.showMessageDialog(this, "申し込みが拒否されました");
			for (int i = 0; i < playerList.size(); i++) {
				buttons1.get(i).setEnabled(true);
				buttons1.get(i).setBackground(Color.WHITE);
				buttons2.get(i).setEnabled(false);
				buttons2.get(i).setBackground(Color.GRAY);
			}
			playSound();

			break;

		case "challenged":
			playSound("notif");
			player.setState(false);

			cm = new ChallengedMenu(this, body);
			cm.setVisible(true);
			switch (cm.res) {
			case 0:
				JOptionPane.showMessageDialog(this, """
						相手がキャンセルしました。
						""");
				playSound();
				player.setState(true);
				break;

			case 1:
				player.setColor(false);
				opponent.setColor(true);
				opponent.setName(body);
				sendMessage("accept:" + body);
				JOptionPane.showMessageDialog(this, "<html><body>" + body
						+ "<br />からの対戦申し込みを承諾しました。"
						+ "<br />ハンデ選択に進みます。"
						+ "</body></html>");
				playSound();
				gotomenu("h1");
				break;

			case 2:
				sendMessage("reject:" + body);
				JOptionPane.showMessageDialog(this, "<html><body>" + body
						+ "<br />からの対戦申し込みを<br />拒否しました。</body></html>");
				playSound("cancel");
				player.setState(true);
				break;
			}

		case "cancelled":
			cm.res = 0;
			cm.dispose();
			break;

		case "hdcp1Res1":
			playSound("notif");
			// 0=ハンデなし先手、1=ハンデなし後手、2=ハンデもらう先手、3=相手にハンデ後手
			switch (body) {
			case "0":
				JOptionPane.showMessageDialog(this, "<html><body>本ゲームはハンデなしとなりました。"
						+ "<br />あなたは先手（黒）です。</body></html>");
				playSound();
				game = new Othello(-1, true);
				player.setColor(true);
				player.setHandi(0);
				opponent.setColor(false);
				opponent.setHandi(0);
				gotomenu("g");
				break;

			case "1":
				JOptionPane.showMessageDialog(this, "<html><body>本ゲームはハンデなしとなりました。"
						+ "<br />あなたは後手（白）です。</body></html>");
				playSound();
				game = new Othello(-1, false);
				player.setColor(false);
				player.setHandi(0);
				opponent.setColor(false);
				opponent.setHandi(0);
				gotomenu("g");
				break;

			case "2":
				JOptionPane.showMessageDialog(this, "<html><body>本ゲームはハンデありとなり、"
						+ "<br />こちらにハンデが与えられました。"
						+ "<br />あなたは先手（黒）です。"
						+ "<br />ハンデレベルの選択に進みます。</body></html>");
				playSound();
				player.setColor(true);
				opponent.setColor(false);
				gotomenu("h2");
				break;

			case "3":
				JOptionPane.showMessageDialog(this, "<html><body>本ゲームはハンデありとなり、"
						+ "<br />相手にハンデが与えられました。"
						+ "<br />あなたは後手（白）です。"
						+ "<br />ハンデレベルの選択に進みます。</body></html>");
				playSound();
				player.setColor(false);
				opponent.setColor(true);
				gotomenu("h2");
				break;
			}
			break;

		case "hdcp2Res1":
			playSound("notif");
			tmp = Integer.parseInt(body);
			boolean hdcp2isDecided = tmp <= 4 ? true : false;
			if (hdcp2isDecided) {
				JOptionPane.showMessageDialog(this, """
						本ゲームのハンデは
						""" + s_hdcp[tmp] + """

						となりました。対局に進みます。
						""");
				playSound();
				player.setHandi(tmp);
				game = new Othello(tmp, player.getColor());
				game.print();
				gotomenu("g");
			} else {
				tmp2 = Integer.parseInt(body);
				tmp = JOptionPane.showConfirmDialog(this, """
						ハンデの希望が一致しませんでした。
						""" + "相手の希望：" + s_hdcp[tmp2 - 5] + """
						相手のハンデに同意しますか？
						""", "メッセージ", JOptionPane.YES_NO_OPTION);
				playSound();
				String ans = tmp == JOptionPane.YES_OPTION ? "true" : "false";
				sendMessage("hdcp2Responce:" + ans);
			}

			break;

		case "hdcp2Res2":
			playSound("notif");
			tmp = Integer.parseInt(body);
			if (tmp >= 0 && tmp <= 4) {
				JOptionPane.showMessageDialog(this, """
						本ゲームのハンデは

						""" + s_hdcp[tmp] + """

						となりました。対局に進みます。
						""");
				playSound();
				player.setHandi(Integer.parseInt(body));
				game = new Othello(tmp, player.getColor());
				gotomenu("g");
			} else {
				JOptionPane.showMessageDialog(this, """
						相手との希望が一致しませんでした。
						""" + "あと" + (8 - tmp) + "回繰り返します。" + """
						もう一度ハンデを選んでください。
						""");
				playSound();
				for (int i = 0; i < hdcpIcon.length; i++) {
					hdcp[i].setEnabled(true);
					revalidate();
					repaint();
				}
			}
			break;

		case "put":
			int num = Integer.parseInt(body);
			int y = num / 10;
			int x = num % 10;
			game.playGame(x, y);
			game.print();
			repaintGame();
			playSound("put");
			game.checkOver();
			break;

		case "pass":
			game.toPass();
			repaintGame();
			game.checkOver();
			break;

		case "resign":
			playSound("notif");
			JOptionPane.showMessageDialog(this, """
					相手が投了しました。
					ゲームを終了します。
					""");
			System.exit(0);
			break;

		case "disconnected":
			JOptionPane.showMessageDialog(this, """
					対戦相手の接続が切れました。
					ゲームを終了します。
					""");
			System.exit(0);

			break;
		}
	}

	private class ChallengedMenu extends JDialog implements ActionListener {
		private JPanel p = new JPanel(new FlowLayout());
		private JButton ok, cancel;
		private JLabel label;
		private int res = 2; // 0:キャンセル、1:accept、2:reject -1:disconnected

		public ChallengedMenu(Client c, String name) {
			super(c, ModalityType.APPLICATION_MODAL);

			setTitle("リバーシ　対戦申し込みが来ました");
			setResizable(false);

			p.setPreferredSize(new Dimension(280, 100));
			label = new JLabel("<html><body>" + name + "<br />からの対戦申し込みです。"
					+ "<br />このプレイヤと対戦しますか？</body></html>");
			label.setPreferredSize(new Dimension(240, 50));
			label.setHorizontalAlignment(JLabel.CENTER);
			ok = new JButton("はい");
			ok.addActionListener(this);
			cancel = new JButton("いいえ");
			cancel.addActionListener(this);

			p.add(label);
			p.add(ok);
			p.add(cancel);

			add(p);
			pack();
		}

		public void setVisible(boolean b) {
			setLocation(client.getX() + client.getWidth() / 2 - getWidth() / 2,
					client.getY() + client.getHeight() / 2 - getHeight() / 2);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			super.setVisible(b);
		}

		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "はい":
				if (res > 0)
					res = 1;
				playSound();
				setVisible(false);
				break;

			case "いいえ":
				if (res > 0)
					res = 2;
				playSound("cancel");
				setVisible(false);
			}
		}
	}

	private class LoginMenu extends JDialog implements ActionListener {
		private JPanel p;
		private JButton ok, cancel;
		private JLabel label, l2, l3;
		private JTextField tf, tf2, tf3;
		private int state = 0;
		private String name;

		public LoginMenu(Client client, ModalityType mt) {
			super(client, mt);

			// >>>>>> ウィンドウパラメータのデフォルト値等を設定 >>>>>>
			setTitle("リバーシ　名前の入力");
			setLocation(client.getX() + client.getWidth() / 2 - getWidth() / 2,
					client.getY() + client.getHeight() / 2 - getHeight() / 2);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setResizable(false);
			// <<<<<<

			// >>>>>> GUI部品を用意 >>>>>>
			p = new JPanel(new FlowLayout());
			p.setPreferredSize(new Dimension(250, 200));
			label = new JLabel("名前（空ならNoName）");
			label.setPreferredSize(new Dimension(240, 20));
			label.setHorizontalAlignment(JLabel.CENTER);
			l2 = new JLabel("IPアドレス（空ならlocalhost）");
			l2.setPreferredSize(new Dimension(240, 20));
			l2.setHorizontalAlignment(JLabel.CENTER);
			l3 = new JLabel("ポート番号（空なら10000）");
			l3.setPreferredSize(new Dimension(240, 20));
			l3.setHorizontalAlignment(JLabel.CENTER);
			ok = new JButton("OK");
			ok.addActionListener(this);
			cancel = new JButton("取消");
			cancel.setEnabled(false);
			cancel.addActionListener(this);
			tf = new JTextField(20);
			tf2 = new JTextField(20);
			tf3 = new JTextField(20);

			p.add(label);
			p.add(tf);
			p.add(l2);
			p.add(tf2);
			p.add(l3);
			p.add(tf3);
			p.add(ok);
			p.add(cancel);

			add(p);
			pack();
			// <<<<<<
		}

		public void setVisible(boolean b) {
			setLocation(client.getX() + client.getWidth() / 2 - getWidth() / 2,
					client.getY() + client.getHeight() / 2 - getHeight() / 2);
			super.setVisible(b);
		}

		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "OK":
				if (state == 0) {
					state = 1;
					name = tf.getText();
					if (name.equals("")) {
						name = "NoName";
					}
					label.setText("ログイン名「" + name + "」でいいですか？");
					cancel.setEnabled(true);
					tf.setEnabled(false);
					tf2.setEnabled(false);
					tf3.setEnabled(false);

				} else {
					String sAddress = tf2.getText().equals("") ? "localhost" : tf2.getText();
					String port = tf3.getText().equals("") ? "10000" : tf3.getText();
					client.connectServer(sAddress, Integer.parseInt(port));
					client.sendMessage("name:" + name);
					setVisible(false);
				}
				break;

			case "取消":
				state = 0;
				label.setText("名前を入力してください");
				cancel.setEnabled(false);
				tf.setEnabled(true);
				tf2.setEnabled(true);
				tf3.setEnabled(true);
			}
		}
	}

	private class Receiver extends Thread {
		private BufferedReader br;

		Receiver(Socket socket) {
			try {
				br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}

		public void run() {
			try {
				while (true) {
					String inputLine = br.readLine();
					if (inputLine != null) {
						receiveMessage(inputLine);
					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}
	}

	private int autoScale(int in) {
		return (int) ((double) in * size_r);
	}

	private void adjust(Component cmp, int x, int y, int width, int height) {
		cmp.setBounds(autoScale(x), autoScale(y), autoScale(width), autoScale(height));
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand(), header, body = null;
		if (command.indexOf(':') == -1)
			header = command;
		else {
			header = command.substring(0, command.indexOf(':'));
			body = command.substring(header.length() + 1);
		}

		// 音出し、volumeクリックの処理
		switch (header) {
		case "volume":
			if (volume_enabled)
				volume.setIcon(volumeIconOff);
			else {
				audios.get("default").setFramePosition(0);
				audios.get("default").start();
				volume.setIcon(volumeIconOn);
			}
			volume_enabled = !volume_enabled;
			break;

		case "cancelto":
		case "pass":
			playSound("cancel");
			break;

		case "put":
			playSound("put");
			break;

		default:
			playSound("default");
			break;
		}

		// 処理
		switch (header) {
		case "challengeto":
			player.setColor(true);
			player.setState(false);
			opponent.setColor(false);
			opponent.setName(body);
			sendMessage("challengeto:" + body);
			tmp = playerList.indexOf(body);
			for (int i = 0; i < playerList.size(); i++) {
				buttons1.get(i).setEnabled(false);
				buttons1.get(i).setBackground(Color.GRAY);
			}
			buttons2.get(tmp).setEnabled(true);
			break;

		case "cancelto":
			player.setState(true);
			sendMessage("cancelto:" + body);
			for (int i = 0; i < playerList.size(); i++) {
				buttons1.get(i).setEnabled(true);
				buttons1.get(i).setBackground(Color.WHITE);
				buttons2.get(i).setEnabled(false);
				buttons2.get(i).setBackground(Color.GRAY);
			}
			break;

		case "hope_hdcp":
			tmp = JOptionPane.showConfirmDialog(this, """
					ハンデありとなった場合、
					先手・後手が入れ替わることがあります。
					よろしいですか？""", "メッセージ", JOptionPane.YES_NO_OPTION);
			if (tmp == JOptionPane.YES_OPTION) {
				playSound();
				sendMessage("hdcpExist:true");
				button1.setEnabled(false);
				button1.setBackground(Color.gray);
				button2.setEnabled(false);
				button2.setBackground(Color.gray);
			} else
				playSound("cancel");
			break;

		case "hope_nohdcp":
			tmp = JOptionPane.showConfirmDialog(this, "<html><body>相手がハンデを希望した場合は"
					+ "<br />相手にハンデが与えられます。"
					+ "<br />よろしいですか？。</body></html>", "メッセージ", JOptionPane.YES_NO_OPTION);
			if (tmp == JOptionPane.YES_OPTION) {
				playSound();
				sendMessage("hdcpExist:false");
				button1.setEnabled(false);
				button1.setBackground(Color.gray);
				button2.setEnabled(false);
				button1.setBackground(Color.white);
			} else
				playSound("cancel");

			break;

		case "hdcp":
			tmp = Integer.parseInt(body);
			tmp2 = JOptionPane.showConfirmDialog(this, s_hdcp[tmp] + """

					を選択しますか？
					""", "メッセージ", JOptionPane.YES_NO_OPTION);
			if (tmp2 == JOptionPane.YES_OPTION) {
				playSound();
				sendMessage("hdcp2Hope:" + tmp);
				for (int i = 0; i < hdcpIcon.length; i++) {
					hdcp[i].setEnabled(false);
				}
				revalidate();
				repaint();
			} else
				playSound("cancel");
			break;

		case "put":
			int num = Integer.parseInt(body);
			int y = num / 10;
			int x = num % 10;
			if (game.grids.get(y).get(x) == 3 && game.getTurn() == player.getColor()) {
				if (isLoggedin)
					sendMessage("put:" + Integer.toString(y) + Integer.toString(x));
				game.playGame(x, y);
				repaintGame();
				game.checkOver();
			}
			break;

		case "pass":
			if (isLoggedin)
				sendMessage("pass");
			game.toPass();
			repaintGame();
			game.checkOver();
			break;

		case "resign":
			tmp = JOptionPane.showConfirmDialog(
					this, "投了しますか？", "メッセージ", JOptionPane.YES_NO_OPTION);
			if (tmp == JOptionPane.YES_OPTION) {
				playSound();
				if (isLoggedin)
					sendMessage("resign");
				JOptionPane.showMessageDialog(this, """
						投了しました。
						対戦を終了します。
						""");
				System.exit(0);
			} else
				playSound("cancel");

			break;
		}

	}

	private void playSound(String str) {
		if (volume_enabled) {
			audios.get(str).setFramePosition(0);
			audios.get(str).start();
		}
	}

	private void playSound() {
		if (volume_enabled) {
			audios.get("default").setFramePosition(0);
			audios.get("default").start();
		}
	}

	private void addPlayerName(String name) {
		System.out.println("プレイヤ名：" + name);
		player.setName(name);
		client.gotomenu("m");
	}

	private void gotomenu(String s) { // 画面の更新

		Insets insets = getInsets();
		size_r = (double) (this.getHeight() - insets.top - insets.bottom) / (double) DEFH;
		switch (s) {
		case "t":
			this.getContentPane().removeAll();
			this.setContentPane(screens.get("title"));
			break;

		case "m":
			this.getContentPane().removeAll();
			screens.put("match", new Match());
			this.setContentPane(screens.get("match"));
			break;

		case "h1":
			this.getContentPane().removeAll();
			screens.put("hdcp1", new Hdcp1());
			this.setContentPane(screens.get("hdcp1"));
			break;

		case "h2":
			this.getContentPane().removeAll();
			screens.put("hdcp2", new Hdcp2());
			this.setContentPane(screens.get("hdcp2"));
			break;

		case "g":
			this.getContentPane().removeAll();
			screens.put("game", new Game());
			this.setContentPane(screens.get("game"));
			break;
		}
	}

	public static void main(String args[]) throws InterruptedException {
		Client client = new Client();
		client.setVisible(true);
	}
}