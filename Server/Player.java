

public class Player{

	private  String myName = "";		//プレイヤ名
	private boolean myState = true;	//プレイヤ状態(待機中を真とする)
	private int myHandi = -1;	//ハンデ情報
	private  boolean myColor = true;	//先手後手情報(黒を真とする)

	// メソッド
	public  void setName(String name){	// プレイヤ名を入力
		myName = name;
	}

	public String getName(){	// プレイヤ名を取得
		return myName;
	}

	public void setState(Boolean state) {	//プレイヤ状態を変更
		myState = state;
	}

	public boolean getState() {	//プレイヤ状態を取得
		return myState;
	}

	public void setColor(boolean c){	// 先手後手情報の受付
		myColor = c;
	}

	public  boolean getColor(){	// 先手後手情報の取得
		return myColor;
	}

	public void setHandi(int handi) {	//ハンデ情報の変更
		myHandi = handi;
	}

	public int getHandi() {	//ハンデ情報の取得
		return myHandi;
	}

}
