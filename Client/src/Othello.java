
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

public class Othello {

	public static boolean turn = true; // 手番(自分の番の時を真とする)
	public int color; // 自身のコマの色
	public int handi; //本ゲームで使用されるハンデ
	public int bStone; // 黒のコマの個数
	public int wStone; // 白のコマの個数
	public int aSpace; // 置ける場所の個数
	public boolean pass = false; // パスの可否(パスできる時を真とする)

	// 反転の判定
	boolean flagTop = false; // ひっくり返せない場合を偽とする
	boolean flagBottom = false;
	boolean flagLeft = false;
	boolean flagRight = false;
	boolean flagTopLeft = false;
	boolean flagBottomLeft = false;
	boolean flagTopRight = false;
	boolean flagBottomRight = false;

	// 局面(空白0,黒1,白2,置ける場所3)
	public List<List<Integer>> grids = new ArrayList<List<Integer>>(Arrays.asList(
			Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0),
			Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0),
			Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0),
			Arrays.asList(0, 0, 0, 2, 1, 0, 0, 0),
			Arrays.asList(0, 0, 0, 1, 2, 0, 0, 0),
			Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0),
			Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0),
			Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0)));

	// 石の周囲の探索用
	static int dir[][] = { { 0, 1 }, { 1, 0 }, { -1, 0 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

	// コンストラクタ
	public Othello(int x, boolean turn) { // xはハンデ情報,未完

		setHdcp(x, turn);

		checkSpace(); // 置ける場所を計算
		print();

	}

	public void print() { // テスト用
		for (int i = 0; i < 8; i++) {
			System.out.println(grids.get(i));
		}
		System.out.print("\n");
	}

	public int countStones(int c) { // パステスト用
		int count = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (grids.get(i).get(j) == c) {
					count++;
				}
			}
		}
		return count;
	}

	public void setHdcp(int x, boolean turn) {
		wStone = 2;
		handi = x;
		// this.turn = turn;
		if (!turn) { // 白ならば
			color = 2;
		} else { // 黒ならば
			color = 1;
		}

		switch (x) {
		case -1:
		case 0:
			bStone = 2;
			break;
		case 1:
			bStone = 3;
			grids.get(0).set(0, 1);
			break;
		case 2:
			bStone = 4;
			grids.get(0).set(0, 1);
			grids.get(7).set(7, 1);
			break;
		case 3:
			bStone = 5;
			grids.get(0).set(0, 1);
			grids.get(7).set(7, 1);
			grids.get(0).set(7, 1);
			break;
		case 4:
			bStone = 6;
			grids.get(0).set(0, 1);
			grids.get(7).set(7, 1);
			grids.get(0).set(7, 1);
			grids.get(7).set(0, 1);
			break;
		}
	}

	public boolean getTurn() { // 手番情報を取得
		return turn;
	}

	public void changeTurn() { // 手番を変更
		if (getTurn()) {
			turn = false;
		} else {
			turn = true;
		}
	}

	public void playGame(int x, int y) {
		addStone(x, y);
		clearSpace();
		checkSpace();
	}

	public void toPass() {
		System.out.println("changed");
		changeTurn();
		clearSpace();
		checkSpace();
	}

	private static int turnToColor(boolean b) {
		return b ? 1 : 2;
	}

	public void addStone(int x, int y) {// 石を置く
		if (grids.get(y).get(x) == 3) {
			grids.get(y).set(x, turnToColor(turn));
			if (turnToColor(turn) == 1) {
				bStone++;
			} else {
				wStone++;
			}
			if (checkReverse(x, y)) {
				Reverse(x, y);
			}
			changeTurn();
		} else {
			System.out.println("ここにはおけません");
		}
	}

	public void reduceStone() {
		if (turnToColor(turn) == 1) {
			bStone++;
			wStone--;
		} else {
			bStone--;
			wStone++;
		}
	}

	public void clearSpace() { // 置ける場所(3)の記号の削除
		aSpace = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (grids.get(i).get(j) == 3) {
					grids.get(i).set(j, 0);
				}
			}
		}
	}

	public void checkAround(int x, int y) { // (x,y)周囲の置ける場所の探索
		for (int i = 0; i < 8; i++) {
			int dx = x + dir[i][0];
			int dy = y + dir[i][1];
			if (dx >= 0 && dx <= 7 && dy >= 0 && dy <= 7) {
				if (grids.get(dy).get(dx) != 1) {
					if (grids.get(dy).get(dx) != 2) {
						if (checkReverse(dx, dy)) {
							aSpace++;
							grids.get(dy).set(dx, 3);
						}
					}
				}
			}
			if (aSpace == 0) {
				pass = true;
			} else {
				pass = false;
			}
		}
	}

	public void checkSpace() { // 置ける場所の全探索
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (grids.get(i).get(j) == 1 || grids.get(i).get(j) == 2) {
					checkAround(j, i);
				}
			}
		}
	}

	// (x,y)にc色の石を置いたことでひっくり返せる石はあるかの探索
	public boolean checkReverse(int x, int y) {

		int count;
		int tempX;
		int tempY;
		// フラッグのリセット
		flagTop = false;
		flagBottom = false;
		flagLeft = false;
		flagRight = false;
		flagTopLeft = false;
		flagBottomLeft = false;
		flagTopRight = false;
		flagBottomRight = false;

		// 上
		count = 0;
		for (int i = y - 1; i >= 0; i--) {
			count++;
			if (grids.get(i).get(x) == 0)
				break; // スペースなら終了
			if (grids.get(i).get(x) == 3)
				break;
			if (grids.get(i).get(x) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(i).get(x) == turnToColor(turn) && count > 1) {
				flagTop = true;
				break;
			}
		}

		// 下
		count = 0;
		for (int i = y + 1; i <= 7; i++) {
			count++;
			if (grids.get(i).get(x) == 0)
				break; // スペースなら終了
			if (grids.get(i).get(x) == 3)
				break;
			if (grids.get(i).get(x) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(i).get(x) == turnToColor(turn) && count > 1) {
				flagBottom = true;
				break;
			}
		}

		// 左
		count = 0;
		for (int i = x - 1; i >= 0; i--) {
			count++;
			if (grids.get(y).get(i) == 0)
				break; // スペースなら終了
			if (grids.get(y).get(i) == 3)
				break;
			if (grids.get(y).get(i) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(y).get(i) == turnToColor(turn) && count > 1) {
				flagLeft = true;
				break;
			}
		}

		// 右
		count = 0;
		for (int i = x + 1; i <= 7; i++) {
			count++;
			if (grids.get(y).get(i) == 0)
				break; // スペースなら終了
			if (grids.get(y).get(i) == 3)
				break;
			if (grids.get(y).get(i) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(y).get(i) == turnToColor(turn) && count > 1) {
				flagRight = true;
				break;
			}
		}

		// 左上
		count = 0;
		tempX = x;
		tempY = y;
		while (tempX > 0 && tempY > 0) {
			count++;
			tempX--;
			tempY--;
			if (grids.get(tempY).get(tempX) == 0)
				break;
			if (grids.get(tempY).get(tempX) == 3)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count > 1) {
				flagTopLeft = true;
				break;
			}
		}

		// 左下
		count = 0;
		tempX = x;
		tempY = y;
		while (tempX > 0 && tempY < 7) {
			count++;
			tempX--;
			tempY++;
			if (grids.get(tempY).get(tempX) == 0)
				break;
			if (grids.get(tempY).get(tempX) == 3)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count > 1) {
				flagBottomLeft = true;
				break;
			}
		}

		// 右上
		count = 0;
		tempX = x;
		tempY = y;
		while (tempX < 7 && tempY > 0) {
			count++;
			tempX++;
			tempY--;
			if (grids.get(tempY).get(tempX) == 0)
				break;
			if (grids.get(tempY).get(tempX) == 3)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count > 1) {
				flagTopRight = true;
				break;
			}
		}

		// 右下
		count = 0;
		tempX = x;
		tempY = y;
		while (tempX < 7 && tempY < 7) {
			count++;
			tempX++;
			tempY++;
			if (grids.get(tempY).get(tempX) == 0)
				break;
			if (grids.get(tempY).get(tempX) == 3)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count == 1)
				break;
			if (grids.get(tempY).get(tempX) == turnToColor(turn) && count > 1) {
				flagBottomRight = true;
				break;
			}
		}
		if (flagTop || flagBottom || flagLeft || flagRight ||
				flagTopLeft || flagBottomLeft || flagTopRight || flagBottomRight) {
			return true;
		}

		return false;

	}

	public void Reverse(int x, int y) { // 石を反転させる

		int count;
		int tempX;
		int tempY;
		// 上
		if (flagTop) {
			count = 0;
			for (int i = y - 1; i >= 0; i--) {
				count++;
				if (grids.get(i).get(x) == 0)
					break;
				if (grids.get(i).get(x) == 3)
					break;
				if (count > 1 && grids.get(i).get(x) == turnToColor(turn)) {
					break;
				}
				grids.get(i).set(x, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 下
		if (flagBottom) {
			count = 0;
			for (int i = y + 1; i <= 7; i++) {
				count++;
				if (grids.get(i).get(x) == 0)
					break;
				if (grids.get(i).get(x) == 3)
					break;
				if (count > 1 && grids.get(i).get(x) == turnToColor(turn)) {
					break;
				}
				grids.get(i).set(x, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 左
		if (flagLeft) {
			count = 0;
			for (int i = x - 1; i >= 0; i--) {
				count++;
				if (grids.get(y).get(i) == 0)
					break;
				if (grids.get(y).get(i) == 3)
					break;
				if (count > 1 && grids.get(y).get(i) == turnToColor(turn)) {
					break;
				}
				grids.get(y).set(i, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 右
		if (flagRight) {
			count = 0;
			for (int i = x + 1; i <= 7; i++) {
				count++;
				if (grids.get(y).get(i) == 0)
					break;
				if (grids.get(y).get(i) == 3)
					break;
				if (count > 1 && grids.get(y).get(i) == turnToColor(turn)) {
					break;
				}
				grids.get(y).set(i, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 左上
		if (flagTopLeft) {
			count = 0;
			tempX = x;
			tempY = y;
			while (tempX > 0 && tempY > 0) {
				count++;
				tempX--;
				tempY--;
				if (grids.get(tempY).get(tempX) == 0)
					break;
				if (grids.get(tempY).get(tempX) == 3)
					break;
				if (count > 1 && grids.get(tempY).get(tempX) == turnToColor(turn)) {
					break;
				}
				grids.get(tempY).set(tempX, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 左下
		if (flagBottomLeft) {
			count = 0;
			tempX = x;
			tempY = y;
			while (tempX > 0 && tempY < 7) {
				count++;
				tempX--;
				tempY++;
				if (grids.get(tempY).get(tempX) == 0)
					break;
				if (grids.get(tempY).get(tempX) == 3)
					break;
				if (count > 1 && grids.get(tempY).get(tempX) == turnToColor(turn)) {
					break;
				}
				grids.get(tempY).set(tempX, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 右上
		if (flagTopRight) {
			count = 0;
			tempX = x;
			tempY = y;
			while (tempX < 7 && tempY > 0) {
				count++;
				tempX++;
				tempY--;
				if (grids.get(tempY).get(tempX) == 0)
					break;
				if (grids.get(tempY).get(tempX) == 3)
					break;
				if (count > 1 && grids.get(tempY).get(tempX) == turnToColor(turn)) {
					break;
				}
				grids.get(tempY).set(tempX, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
		// 右下
		if (flagBottomRight) {
			count = 0;
			tempX = x;
			tempY = y;
			while (tempX < 7 && tempY < 7) {
				count++;
				tempX++;
				tempY++;
				if (grids.get(tempY).get(tempX) == 0)
					break;
				if (grids.get(tempY).get(tempX) == 3)
					break;
				if (count > 1 && grids.get(tempY).get(tempX) == turnToColor(turn)) {
					break;
				}
				grids.get(tempY).set(tempX, turnToColor(turn)); // 反転
				reduceStone();
			}
		}
	}

	public void checkOver() {
		if (isGameover()) { // 終了判断を行う
			String s = "対局終了です。\n";
			if (checkWinner(handi) == 1) {
				s = """
						あなたの勝ち！
						ゲームを終了します。
						""";
			} else if (checkWinner(handi) == -1){
				s = """
						あなたの負け...
						ゲームを終了します。
						""";
			}else {
				s = """
						引き分け
						ゲームを終了します。
						""";
			}
			JOptionPane.showMessageDialog(null, s);
			System.exit(0);
		}
	}

	public boolean isGameover() { // 終了判定
		if (bStone + wStone == 64) { // 石の総数が64または両者ともにパス
			return true;
		} else if (aSpace != 0) {
			return false;
		} else {
			changeTurn();
			checkSpace();
			if (aSpace == 0)
				return true;
			changeTurn();
			return false;
		}
	}

	public int checkWinner(int x){	// 勝敗判定,1勝ち-1負け0引き分け
		switch(x) {
			case -1:
			case 1:
			case 2:
			case 3:
			case 4:
				if(color == 1) {
					if(bStone > wStone) {
						return 1;
					}else if(bStone < wStone){
						return -1;
					}else {
						return 0;
					}
				}else {
					if(wStone > bStone) {
						return 1;
					}else if(wStone < bStone){
						return -1;
					}else {
						return 0;
					}
				}
			case 0:
				if(color == 1) {
					if(bStone >= wStone) {
						return 1;
					}else{
						return -1;
					}
				}else {
					if(wStone > bStone) {
						return 1;
					}else{
						return -1;
					}
				}
		}
		return -2;
	}

}
