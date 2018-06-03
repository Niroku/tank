package map;

import net.sf.json.JSONObject;

public class Player extends Goal {
	private int id;
	private int team;
//	private int x;
//	private int y;
	private int superBullet;
//	private Player goal;// �洢��̹�˵Ĺ���Ŀ��
//	private Coin goalCoin;
	private Goal goal;// Ŀ��

	public Player(JSONObject object) {
		super(object.getInt("x"), object.getInt("y"));
		this.id = object.getInt("id");
		this.team = object.getInt("team");
//		this.x = object.getInt("x");
//		this.y = object.getInt("y");
		this.superBullet = object.getInt("super_bullet");
		System.out.printf("player id %d team %d x %d, y %d, super bullet %d\n", this.id, this.team, getX(), getY(),
				this.superBullet);
	}

	public int getId() {
		return this.id;
	}

	public int getTeam() {
		return this.team;
	}

//	public int getX() {
//		return x;
//	}
//
//	public int getY() {
//		return y;
//	}

	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	public Goal getGoal() {
		return goal;
	}

	public int getSuperBullet() {
		return superBullet;
	}

	@Override
	public String toString() {
		return "player(" + getY() + ", " + getX() + ")";
	}
}
