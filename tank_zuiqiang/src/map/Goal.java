package map;

//Ŀ���࣬��ʾplayer��Ŀ�����
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
		return "Ŀ��(" + y + ", " + x + ")";
	}

	@Override
	public boolean equals(Object obj) {
		return x == ((Goal) obj).getX() && y == ((Goal) obj).getY(); // 2��goal��ȵ�������x��y���
	}

}
