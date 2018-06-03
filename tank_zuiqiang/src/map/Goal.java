package map;

//目标类，表示player的目标对象
public class Goal {
	private int x;
	private int y;

	public Goal(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public String toString() {
		return "目标(" + y + ", " + x + ")";
	}

	@Override
	public boolean equals(Object obj) {
		return x == ((Goal) obj).getX() && y == ((Goal) obj).getY(); // 2个goal相等的条件是x，y相等
	}

}
