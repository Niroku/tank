package demo;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import map.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import demo.Team;
import cmd.Action;
import cmd.RoundAction;

public class Client {
	private int team_id = 0;
	private String team_name = "";
	private Team self = null;
	private Team enemy = null;
	private int roundId = 0;
	private Goal[] goalOfPlayers = new Goal[8]; // 存储4个坦克的目标
	private List<Player> players = new ArrayList<Player>();
	private List<Goal> enemyPlayers = new ArrayList<>();
	private List<Goal> coinsAndStars = new ArrayList<>(); // 存储所有的金币和道具
	private String[][] myMap; // 地图
	private String[] lastDirsOfPlayer = new String[8];
	private int width; // 地图的宽高
	private int height;
	private int coin_count;
	private final int INF = 1000; // 定义无穷，用于初始化最小值
	private final int MIN_BULLET_DIS = 4; // 默认子弹最大搜索距离
	private final int MIN_ENEMY_DIS = 3; // 默认坦克搜索最大距离

	public Client(int team_id, String team_name) {
		this.team_id = team_id;
		this.team_name = team_name;
	}

	public void legStart(JSONObject data) {
		System.out.println("leg start");

		JSONObject map = data.getJSONObject("map");

		width = map.getInt("width"); // 存储地图的高宽
		height = map.getInt("height");

		System.out.printf("map width:%d, map height %d\n", width, height);

		JSONArray teams = data.getJSONArray("teams");

		for (int i = 0; i < 2; i++) {
			JSONObject team = teams.getJSONObject(i);
			int team_id = team.getInt("id");
			if (this.team_id == team_id) {
				System.out.println("self team");
				this.self = new Team(team);
			}
			else {
				System.out.println("enemy team");
				this.enemy = new Team(team);
			}
		}
	}

	public void legEnd(JSONObject data) {
		System.out.println("leg end");

		JSONArray results = data.getJSONArray("teams");
		for (int i = 0; i < results.size(); i++) {
			Result result = new Result(results.getJSONObject(i));
		}
	}

	public void round(JSONObject data) {

		myMap = new String[width][height]; // 初始化地图2维数组，‘+’表示空地
		for (int i = 0; i < myMap.length; i++)
			for (int j = 0; j < myMap[i].length; j++)
				myMap[i][j] = "+";

		this.roundId = data.getInt("round_id");
		System.out.printf("round %d\n", this.roundId);

		JSONArray brickWalls = data.getJSONArray("brick_walls");
		for (int i = 0; i < brickWalls.size(); i++) {
			JSONObject object = brickWalls.getJSONObject(i);
			myMap[object.getInt("y")][object.getInt("x")] = "$";
		}

		JSONArray ironWalls = data.getJSONArray("iron_walls");
		for (int i = 0; i < ironWalls.size(); i++) {
			JSONObject object = ironWalls.getJSONObject(i);
			myMap[object.getInt("y")][object.getInt("x")] = "#";
		}

		JSONArray rivers = data.getJSONArray("river");
		for (int i = 0; i < rivers.size(); i++) {
			JSONObject object = rivers.getJSONObject(i);
			myMap[object.getInt("y")][object.getInt("x")] = "@";
		}

		this.coinsAndStars.clear();
		JSONArray coins = data.getJSONArray("coins");
		coin_count = coins.size();
		for (int i = 0; i < coins.size(); i++) {
			JSONObject object = coins.getJSONObject(i);
			coinsAndStars.add(new Goal(object.getInt("x"), object.getInt("y")));// 将金币和道具看做一样的一起存起来

			myMap[object.getInt("y")][object.getInt("x")] = "c";
		}

		JSONArray stars = data.getJSONArray("stars");
		for (int i = 0; i < stars.size(); i++) {
			JSONObject object = stars.getJSONObject(i);
			coinsAndStars.add(new Goal(object.getInt("x"), object.getInt("y")));// 将金币和道具看做一样的一起存起来
			myMap[object.getInt("y")][object.getInt("x")] = "*";
		}
		// start
		if (coins.size() < 10) {
			this.coinsAndStars.clear();
			for (int i = 0; i < coins.size(); i++) {
				JSONObject object = coins.getJSONObject(i);
				int x = object.getInt("x");
				int y = object.getInt("y");
				if (xiugaimap(x, y)) {
					coinsAndStars.add(new Goal(object.getInt("x"), object.getInt("y")));
				}
			}
			for (int i = 0; i < stars.size(); i++) {
				JSONObject object = stars.getJSONObject(i);
				int x = object.getInt("x");
				int y = object.getInt("y");
				if (xiugaimap(x, y)) {
					coinsAndStars.add(new Goal(object.getInt("x"), object.getInt("y")));
				}
			}
		}
		// end

		JSONArray bullets = data.getJSONArray("bullets");
		for (int i = 0; i < bullets.size(); i++) {
			JSONObject object = bullets.getJSONObject(i);
			myMap[object.getInt("y")][object.getInt("x")] = "b_" + object.getString("direction");

			// 一个子弹的完整的字符串表示为b_up_s(e)

			if (object.getInt("team") == team_id) // 给每个子弹也加上队伍的标识
				myMap[object.getInt("y")][object.getInt("x")] += "_s";
			else
				myMap[object.getInt("y")][object.getInt("x")] += "_e";
		}

		this.players.clear();
		enemyPlayers.clear();
		JSONArray players = data.getJSONArray("players");
		for (int i = 0; i < players.size(); i++) {
			JSONObject object = players.getJSONObject(i);
			Player player = new Player(object);
			if (player.getTeam() == this.team_id) {
				this.players.add(player);
				myMap[object.getInt("y")][object.getInt("x")] = "s";
			}
			else {
				enemyPlayers.add(player);
				myMap[object.getInt("y")][object.getInt("x")] = "e";
			}

		}
	}

	public RoundAction act() {

		printMap();// 打印地图

		if (coinsAndStars.size() >= 3) {// 如果场上金币和道具数大于3
			for (Player player : players) {
				player.setGoal(findNearestGoal(player, coinsAndStars)); // 每回合都更新目标
			}
		}
		else {
			if (coin_count <= 3) {
				Goal commonEnemy = ourNearstEnemy();
				for (Player player : players)
					player.setGoal(commonEnemy);

			}
			else {
				for (Player player : players) {
					player.setGoal(findNearestGoal(player, enemyPlayers));// 每回合都更新目标
				}
			}
		}

		List<Action> actions = new ArrayList<Action>();
		for (Player player : players) {
			String move = moveDir(player, lastDirsOfPlayer[player.getId()]);
			String fdAndsb = fireDirAndSb(player);

			String fire;
			int superBullet;

			if (fdAndsb == null) {
				if (move == null)
					fire = randomDir();
				else
					fire = move;
				superBullet = 0;
			}
			else {
				fire = fdAndsb.substring(0, fdAndsb.length() - 1);
				superBullet = Integer.parseInt(fdAndsb.charAt(fdAndsb.length() - 1) + "");
			}

			lastDirsOfPlayer[player.getId()] = move;// 存储新的移动方向

			actions.add(new Action(player.getTeam(), player.getId(), superBullet, move, fire));
		}

		RoundAction roundAction = new RoundAction(this.roundId, actions);

		return roundAction;
	}

	// 修改地图
	private boolean xiugaimap(int x, int y) {
		if (x != 0 && x != width - 1 && y != height - 1 && y != 0) {
			if (myMap[y + 1][x] != "+" && myMap[y - 1][x] != "+" && myMap[y][x + 1] != "+" && myMap[y][x - 1] != "+") {
				myMap[y][x] = "$";
				return false;
			}
		}
		else if (x == 0) {
			if (y == 0) {
				if (myMap[y + 1][x] != "+" && myMap[y][x + 1] != "+") {
					myMap[y][x] = "$";
					return false;
				}
			}
			else if (y == height - 1) {
				if (myMap[y - 1][x] != "+" && myMap[y][x + 1] != "+") {
					myMap[y][x] = "$";
					return false;
				}
			}
			else {
				if (myMap[y - 1][x] != "+" && myMap[y + 1][x] != "+" && myMap[y][x + 1] != "+") {
					myMap[y][x] = "$";
					return false;
				}
			}
		}
		else if (x == width - 1) {
			if (y == 0) {
				if (myMap[y + 1][x] != "+" && myMap[y][x - 1] != "+") {
					myMap[y][x] = "$";
					return false;
				}
			}
			else if (y == height - 1) {
				if (myMap[y - 1][x] != "+" && myMap[y][x - 1] != "+") {
					myMap[y][x] = "$";
					return false;
				}
			}
			else {
				if (myMap[y - 1][x] != "+" && myMap[y + 1][x] != "+" && myMap[y][x - 1] != "+") {
					myMap[y][x] = "$";
					return false;
				}
			}
		}
		else if (y == 0) {
			if (myMap[y + 1][x] != "+" && myMap[y][x + 1] != "+" && myMap[y][x - 1] != "+") {
				myMap[y][x] = "$";
				return false;
			}
		}
		else if (y == height - 1) {
			if (myMap[y - 1][x] != "+" && myMap[y][x + 1] != "+" && myMap[y][x - 1] != "+") {
				myMap[y][x] = "$";
				return false;
			}
		}
		return true;
	}

	// 移动方向
	private String moveDir(Player player, String lastDir) {
		final int[][] dir = { { 0, 1 }, { 0, -1 }, { -1, 0 }, { 1, 0 } }; // 下上左右四个方向
		String[] direction = { "down", "up", "left", "right" };

		int indexOfLastDir = getLastDirIndex(lastDir);// 获得上个方向的下标

		Goal goal = player.getGoal();

		int x_g = goal.getX(); // 获得对应目标的x，y
		int y_g = goal.getY();

		int x_p = player.getX(); // 获得坦克的x，y
		int y_p = player.getY();

		double min = INF;
		int index = -1;
		for (int i = 0; i < dir.length; i++) {

			if (i == indexOfLastDir)// 不搜索上个方向
				continue;

			int x = x_p + dir[i][0]; // 获得上下左右方向的x，y
			int y = y_p + dir[i][1];

			if (x >= width || y >= height || x < 0 || y < 0) {// 判断是否越过地图边界
				continue;
			}
			else if (!myMap[y][x].equals("+") && !myMap[y][x].equals("c") && !myMap[y][x].equals("*")) { // 如果不是空地且不是coin且不是start，直接跳过，砖墙这里看做不可跨越的障碍，因为开火方向不一定和移动方向一样
				continue;
			}
			else {

				double dis = distance(x, y, x_g, y_g);
				if (dis <= min) {
					min = dis;
					index = i;
				}
			}
		}

		if (index == -1)// 如果没有可行的方向，就返回null
			return null;

		String ret = direction[index];


		// 搜索子弹
		for (int i = x_p - 1; i >= x_p - MIN_BULLET_DIS && ret != null && ret.equals("up") && i >= 0; i--) {
			if (isObstacle(myMap[y_p - 1][i]))
				break;
			if (myMap[y_p - 1][i].equals("b_right_e"))
				ret = null;
		}
		for (int i = x_p + 1; i <= x_p + MIN_BULLET_DIS && ret != null && ret.equals("up") && i < width; i++) {
			if (isObstacle(myMap[y_p - 1][i]))
				break;
			if (myMap[y_p - 1][i].equals("b_left_e"))
				ret = null;
		}

		for (int i = x_p - 1; i >= x_p - MIN_BULLET_DIS && ret != null && ret.equals("down") && i >= 0; i--) {
			if (isObstacle(myMap[y_p + 1][i]))
				break;
			if (myMap[y_p + 1][i].equals("b_right_e"))
				ret = null;
		}
		for (int i = x_p + 1; i <= x_p + MIN_BULLET_DIS && ret != null && ret.equals("down") && i < width; i++) {
			if (isObstacle(myMap[y_p + 1][i]))
				break;
			if (myMap[y_p + 1][i].equals("b_left_e"))
				ret = null;
		}

		for (int i = y_p - 1; i >= y_p - MIN_BULLET_DIS && ret != null && ret.equals("left") && i >= 0; i--) {
			if (isObstacle(myMap[i][x_p - 1]))
				break;
			if (myMap[i][x_p - 1].equals("b_down_e"))
				ret = null;
		}
		for (int i = y_p + 1; i <= y_p + MIN_BULLET_DIS && ret != null && ret.equals("left") && i < height; i++) {
			if (isObstacle(myMap[i][x_p - 1]))
				break;
			if (myMap[i][x_p - 1].equals("b_up_e"))
				ret = null;
		}

		for (int i = y_p - 1; i >= y_p - MIN_BULLET_DIS && ret != null && ret.equals("right") && i >= 0; i--) {
			if (isObstacle(myMap[i][x_p + 1]))
				break;
			if (myMap[i][x_p + 1].equals("b_down_e"))
				ret = null;
		}
		for (int i = y_p + 1; i <= y_p + MIN_BULLET_DIS && ret != null && ret.equals("right") && i < height; i++) {
			if (isObstacle(myMap[i][x_p + 1]))
				break;
			if (myMap[i][x_p + 1].equals("b_up_e"))
				ret = null;
		}

		if (coin_count != 0) {
			// 搜索坦克
			for (int i = x_p - 1; i >= x_p - MIN_ENEMY_DIS - 1 && ret != null && ret.equals("up") && i >= 0; i--) {
				if (isObstacle(myMap[y_p - 1][i]))
					break;
				if (myMap[y_p - 1][i].equals("e"))
					ret = null;
			}
			for (int i = x_p + 1; i <= x_p + MIN_ENEMY_DIS + 1 && ret != null && ret.equals("up") && i < width; i++) {
				if (isObstacle(myMap[y_p - 1][i]))
					break;
				if (myMap[y_p - 1][i].equals("e"))
					ret = null;
			}

			for (int i = x_p - 1; i >= x_p - MIN_ENEMY_DIS - 1 && ret != null && ret.equals("down") && i >= 0; i--) {
				if (isObstacle(myMap[y_p + 1][i]))
					break;
				if (myMap[y_p + 1][i].equals("e"))
					ret = null;
			}
			for (int i = x_p + 1; i <= x_p + MIN_ENEMY_DIS + 1 && ret != null && ret.equals("down") && i < width; i++) {
				if (isObstacle(myMap[y_p + 1][i]))
					break;
				if (myMap[y_p + 1][i].equals("e"))
					ret = null;
			}

			for (int i = y_p - 1; i >= y_p - MIN_ENEMY_DIS - 1 && ret != null && ret.equals("left") && i >= 0; i--) {
				if (isObstacle(myMap[i][x_p - 1]))
					break;
				if (myMap[i][x_p - 1].equals("e"))
					ret = null;
			}
			for (int i = y_p + 1; i <= y_p + MIN_ENEMY_DIS + 1 && ret != null && ret.equals("left")
					&& i < height; i++) {
				if (isObstacle(myMap[i][x_p - 1]))
					break;
				if (myMap[i][x_p - 1].equals("e"))
					ret = null;
			}

			for (int i = y_p - 1; i >= y_p - MIN_ENEMY_DIS - 1 && ret != null && ret.equals("right") && i >= 0; i--) {
				if (isObstacle(myMap[i][x_p + 1]))
					break;
				if (myMap[i][x_p + 1].equals("e"))
					ret = null;
			}
			for (int i = y_p + 1; i <= y_p + MIN_ENEMY_DIS + 1 && ret != null && ret.equals("right")
					&& i < height; i++) {
				if (isObstacle(myMap[i][x_p + 1]))
					break;
				if (myMap[i][x_p + 1].equals("e"))
					ret = null;
			}
		}

		return ret;
	}

	// 开火方向，是否发射超级子弹
	private String fireDirAndSb(Player player) {
		String dir = null;
		int superBullet = 0;
		int x = player.getX();
		int y = player.getY();
		boolean isBullet = false;

		// 先搜索子弹，再搜索坦克

		// 以player为中心，分别向四个方向从里向外搜索子弹，如果发现障碍就跳过这个方向的搜索，否则发现子弹就向其开火
		for (int i = y - 1; i >= y - MIN_BULLET_DIS && dir == null && i >= 0; i--) {
			if (isObstacle1(myMap[i][x]))// 如果是障碍，就不管
				break;
			if (myMap[i][x].equals("b_down_e")) {
				dir = "up";
				isBullet = true;
			}
		}
		for (int i = y + 1; i <= y + MIN_BULLET_DIS && dir == null && i < height; i++) {
			if (isObstacle1(myMap[i][x]))
				break;
			if (myMap[i][x].equals("b_up_e")) {
				dir = "down";
				isBullet = true;
			}
		}
		for (int i = x - 1; i >= x - MIN_BULLET_DIS && dir == null && i >= 0; i--) {
			if (isObstacle1(myMap[y][i]))
				break;
			if (myMap[y][i].equals("b_right_e")) {
				dir = "left";
				isBullet = true;
			}
		}
		for (int i = x + 1; i <= x + MIN_BULLET_DIS && dir == null && i < width; i++) {
			if (isObstacle1(myMap[y][i]))
				break;
			if (myMap[y][i].equals("b_left_e")) {
				dir = "right";
				isBullet = true;
			}
		}

		// 以player为中心，分别向四个方向从里向外搜索敌方坦克，如果发现障碍就跳过这个方向的搜索，否则发现敌方坦克就向其开火
		// 如果有超级子弹，就缩小搜索距离
		for (int i = y - 1; i >= y - MIN_ENEMY_DIS + player.getSuperBullet() && dir == null && i >= 0; i--) {
			if (isObstacle1(myMap[i][x]))// 如果是障碍，就不管
				break;
			if (myMap[i][x].equals("e")) {
				dir = "up";
			}
		}
		for (int i = y + 1; i <= y + MIN_ENEMY_DIS - player.getSuperBullet() && dir == null && i < height; i++) {
			if (isObstacle1(myMap[i][x]))
				break;
			if (myMap[i][x].equals("e")) {
				dir = "down";
			}
		}
		for (int i = x - 1; i >= x - MIN_ENEMY_DIS + player.getSuperBullet() && dir == null && i >= 0; i--) {
			if (isObstacle1(myMap[y][i]))
				break;
			if (myMap[y][i].equals("e")) {
				dir = "left";
			}
		}
		for (int i = x + 1; i <= x + MIN_ENEMY_DIS - player.getSuperBullet() && dir == null && i < width; i++) {
			if (isObstacle1(myMap[y][i]))
				break;
			if (myMap[y][i].equals("e")) {
				dir = "right";
			}
		}

		// 如果确定向子弹和坦克发射子弹
		if (dir != null) {
			if (isBullet) { // 如果开火目标是子弹，则默认不发射超级子弹
				superBullet = 0;
			}
			else {
				superBullet = player.getSuperBullet();// 如果向坦克开火，有超级子弹就发射超级子弹
			}
			dir += superBullet;
			return dir;
		}

		if (dir != null)
			dir += superBullet;

		return dir;

	}

	// 判断地图某元素是否是障碍(砖墙，铁墙，我方坦克)
	private boolean isObstacle(String s) {
		return s.equals("$") || s.equals("#") || s.equals("s");
	}

	// 断地图某元素是否是障碍(砖墙，铁墙)
	private boolean isObstacle1(String s) {
		return s.equals("$") || s.equals("#");
	}

	// 返回距离player最近的目标
	private Goal findNearestGoal(Player player, List<Goal> goals) {
		double min = INF;
		int index = 0;
		for (int i = 0; i < goals.size(); i++) {

			if (myMap[goals.get(i).getY()][goals.get(i).getX()].equals("*")) {
				if (player.getSuperBullet() != 0) // 如果已经拥有了超级子弹就跳过，不把星星加入比较
					continue;

				if (enemyPlayers.size() <= 3)// 如果场上敌人数小于等于3，大家都去吃星星
					return goals.get(i);

			}
			else {
				if (new Random().nextInt(10) <= 1) {// 每次都以极小的概率跳过
					continue;
				}
			}

			double dis = distance(player, goals.get(i));

			if (dis < min) {
				min = dis;
				index = i;
			}
		}

		return goals.get(index);
	}

	// 返回我方坦克和目标之间的距离，如果到不了，返回INF
	private double distance(Player player, Goal goal) {
		int x1 = player.getX();
		int y1 = player.getY();
		int x2 = goal.getX();
		int y2 = goal.getY();

		if (x1 == x2) {
			if (y1 == y2)
				return INF;
			if (myMap[y2 + (y1 - y2) / Math.abs(y1 - y2)][x2].equals("$")) {
				return INF;
			}
		}

		else if (y1 == y2) {
			if (myMap[y2][x2 + (x1 - x2) / Math.abs(x1 - x2)].equals("$"))
				return INF;
		}
		else {
			if (myMap[y2][x2 + (x1 - x2) / Math.abs(x1 - x2)].equals("$")
					&& myMap[y2 + (y1 - y2) / Math.abs(y1 - y2)][x2].equals("$"))
				return INF;
		}
		return distance(x1, y1, x2, y2);
	}

	// 计算2点之间的距离（欧几里得距离）
	public static double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}

	private String randomDir() {
		String[] dir = { "up", "down", "left", "right" };
		return dir[new Random().nextInt(dir.length)];
	}

	// 打印地图
	private void printMap() {
		System.out.print("   ");
		for (int i = 0; i < myMap.length; i++)
			System.out.printf("%-3d", i);
		System.out.println();
		for (int i = 0; i < myMap.length; i++) {
			System.out.printf("%-3d", i);
			for (int j = 0; j < myMap[i].length; j++)
				System.out.print(myMap[i][j].charAt(0) + "  ");
			System.out.println();
		}
	}

	// 打印所有目标
	private void printGoal() {
		for (Player player : players) {
			System.out.println(player + "的" + goalOfPlayers[player.getId()]);
		}
	}

	// 目标是否存在
	private boolean goalExist(Goal goal, List<Goal> list) {
		if (goal == null)
			return false;
		for (Goal e : list) {
			if (e.equals(goal))
				return true;
		}
		return false;
	}

	// 获得之前移动返方向对应的下标
	private int getLastDirIndex(String lastDir) {
		if (lastDir == null) // 如果为空返回-1
			return -1;
		String[] dirs = { "up", "down", "right", "left" };
		int i;
		for (i = 0; i < dirs.length; i++)
			if (dirs[i].equals(lastDir))
				break;
		return i;
	}

	// 判断某点是不是角落的点
	private boolean isEdge(int x, int y) {
		if (x == 0 && y == 0)
			return true;
		else if (x == 0 && y == height - 1)
			return true;
		else if (x == width - 1 && y == 0)
			return true;
		else if (x == width - 1 && y == height - 1)
			return true;
		else
			return false;
	}

	// 返回距离我方所有坦克最近的敌人
	private Goal ourNearstEnemy() {
		double min = INF;
		int index = 0;
		for (int i = 0; i < enemyPlayers.size(); i++) {
			double sumDis = 0;
			for (Player player : players)
				sumDis += distance(player, enemyPlayers.get(i));
			if (sumDis < min) {
				min = sumDis;
				index = i;
			}

		}
		return enemyPlayers.get(index);
	}
}