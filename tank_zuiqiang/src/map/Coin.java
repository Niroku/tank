package map;

import net.sf.json.JSONObject;

public class Coin extends Goal {
//	private int x;
//	private int y;
	private int point;

	public Coin(JSONObject object) {
		super(object.getInt("x"), object.getInt("y"));
//		this.x = object.getInt("x");
//		this.y = object.getInt("y");
		this.point = object.getInt("point");
		System.out.printf("star x %d, y %d\n point %d\n", getX(), getY(), this.point);
	}

//	public int getX() {
//		return x;
//	}
//
//	public int getY() {
//		return y;
//	}

}
