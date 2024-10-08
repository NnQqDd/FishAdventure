package map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import main.Main;
import main.SurvivalGame;
import root.IOHandler;

class Tile {
	public BufferedImage image;
	public boolean isConsumable = false;
	public boolean isObstacle = false;
	public boolean isTrap = false;
	public Rectangle solidArea;
	public Tile() {
		solidArea = new Rectangle(0, 0, Main.TILES_SIZE, Main.TILES_SIZE);
	}
}

/*(Current) CODE 
0: Void
1 - 16: Block imgCodes
17: Single Stone
18: Trap
19: Orb
20: Decoration 1
21: Decoration 2
22: Decoration 3
99: Water
*/

public class SurvivalMap {
	private SurvivalGame SG;
	private HashSet<Integer> visit;
	private Queue<Integer> voidTile;
	private int[][] map;
	private int[][] tile;
	private Tile[] imgCode;
	private final int PADD = 1;
	public SurvivalMap(SurvivalGame SG, double obsPerc, double orbPerc, double decoPerc, double trapPerc, double altObsPerc) {
		this.SG = SG;
		imgCode = new Tile[30];
		tile = new int[SurvivalGame.WORLD_ROW][SurvivalGame.WORLD_COL];
		loadSprite();
		createRandomness(obsPerc, orbPerc, decoPerc, trapPerc, altObsPerc);
		loadRandomMap();
	}
	private void loadSprite() {
		BufferedImage img = IOHandler.getImage("map_sprites05.png");
		imgCode[0] = new Tile();
		imgCode[0].image = img.getSubimage(6*512, 0, 512, 512);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				imgCode[i*4 + j + 1] = new Tile();
				imgCode[i*4 + j + 1].image = img.getSubimage(j*512, i*512, 512 , 512);
				imgCode[i*4 + j + 1].isObstacle = true;
			}
		}
		imgCode[17] = new Tile();
		imgCode[17].image = img.getSubimage(6*512, 4*512, 512, 512);
		imgCode[17].isObstacle = true; 
		
		imgCode[18] = new Tile();
		imgCode[18].image = img.getSubimage(4*512, 3*512, 512, 512);
		imgCode[18].isObstacle = true;
		imgCode[18].isTrap = true;
		
		imgCode[19] = new Tile();
		imgCode[19].image = IOHandler.getImage("orb.png");
		imgCode[19].isConsumable = true;
		imgCode[19].isObstacle = false;
		imgCode[19].solidArea = new Rectangle(Main.TILES_SIZE/4, Main.TILES_SIZE/4, Main.TILES_SIZE/2, Main.TILES_SIZE/2);
		
		imgCode[20] = new Tile();
		imgCode[20].image = img.getSubimage(4*512, 6*512, 512, 512);
		
		imgCode[21] = new Tile();
		imgCode[21].image = img.getSubimage(5*512, 6*512, 512, 512);
		
		imgCode[22] = new Tile();
		imgCode[22].image = img.getSubimage(6*512, 6*512, 512, 512);
	}
	public boolean tileIsConsumable(int code) {
		return (code == 19);
	}
	public boolean tileIsMoveable(int code) {
		return (code == 0 || code >= 19);
	}
	public boolean tileIsTrap(int code) {
		return (code == 18);
	}
	private void createRandomness(double obsPerc, double orbPerc, double decoPerc, double trapPerc, double altObsPerc){
		int height = SurvivalGame.WORLD_ROW;
		int width = SurvivalGame.WORLD_COL;
		int px = SG.getPlayer().getWorldX()/Main.TILES_SIZE;
		int py = SG.getPlayer().getWorldY()/Main.TILES_SIZE;
		map = new int[height][width];
		/*Create border*/
		for(int i = 0; i < height; i++){
			for(int j = 0; j < width; j++){
				if(i == 0 || j == 0 || i == height - 1 || j == width - 1){
					map[i][j] = 1;
				}
			}
		} 
		
		for(int i = 1; i < height; i++){
			for(int j = 1; j < width - 1; j += 2){
				if(Math.random() < obsPerc){
					map[i][j] = 1;
					map[i][j + 1] = 1;
				}
			}
		} 
		
		/*Clear area around fish*/
		for(int i = Math.max(px - 2, 1); i <= Math.min(px + 2, height - 2); i++) {
			for(int j = Math.max(py - 2, 1); j <= Math.min(py + 2, width - 2); j++) {
				map[i][j] = 0;
			}
		}

		/*Smooth map*/
		for(int i = 0; i < height; i++){
			for(int j = 0; j < width; j++){
				if(map[i][j] >= 1 && map[i][j] <= 16) {
					int obs = 1;
					if(i <= 0  || map[i - 1][j] != 0) obs += 1; //Up
					if(j >= width - 1 || map[i][j + 1] != 0) obs += 2; //Right
					if(i >= height - 1 || map[i + 1][j] != 0) obs += 4; //Down
					if(j <= 0 || map[i][j - 1] != 0) obs += 8; //Left
					map[i][j] = obs;
				}
			}
		} 
		/*Put decorations & traps & extra obstacles on map*/
		Random ran = new Random();
		for(int i = 1; i < height; i++){
			for(int j = 0; j < width; j++){
				if(map[i - 1][j] == 0 && map[i][j] <= 16 && map[i][j] != 0) {
					if(Math.random() < decoPerc) map[i - 1][j] = ran.nextInt(3) + 20;
					if(Math.random() < altObsPerc) map[i - 1][j] = 17;
					if(Math.random() < trapPerc) map[i - 1][j] = 18;
				}
			}
		}
		/*BFS to find reachable tiles*/
		Queue<Integer> qu = new LinkedList<Integer>();
		visit = new HashSet<Integer>();
		int[][] dirArr = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
		qu.add(py*(width) + px);
		visit.add(py*(width) + px);
		while(qu.size() != 0) {
			int top = qu.poll();
			int row = top/(width);
			int col = top%(width);
			for(int []dir: dirArr) {
				if(row + dir[0] >= 0 && row + dir[0] < height && col + dir[1] >= 0 && col + dir[1] < width
				   && tileIsMoveable(map[row + dir[0]][col + dir[1]])
				   && !visit.contains((row + dir[0])*width + col + dir[1])) {
					visit.add((row + dir[0])*width + col + dir[1]);
					qu.add((row + dir[0])*width + col + dir[1]);
				}
			}
		}

		/*Data structures for placement of coins*/
		voidTile = new LinkedList<Integer>();
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for(int i = 0; i < height*width; i++) {
			arr.add(i);
		}
		Collections.shuffle(arr);
		for(Integer x: arr) {
			if(visit.contains(x)) { 
				if(Math.random() < orbPerc) {
					map[x/width][x%width] = 19;
				}
				else {
					visit.remove(x);
					voidTile.add(x);
				}
			}
		}
	} 
	private void loadRandomMap() {
		for(int i = 0; i < SurvivalGame.WORLD_ROW; i++) {
			for(int j = 0; j < SurvivalGame.WORLD_COL; j++) {
				if(map[i][j] >= 17)
					tile[i][j] = map[i][j]; 
				switch(map[i][j]) {
					case 0:
						tile[i][j] = 0;
						break;
					case 1:
						tile[i][j] = 3*4 + 3 + 1;
						break;
					case 2:
						tile[i][j] = 2*4 + 3 + 1;
						break;
					case 3:
						tile[i][j] = 3*4 + 0 + 1;
						break;
					case 4: //-1 = 1 + 2
						tile[i][j] = 2*4 + 0 + 1;
						break;
					case 5:
						tile[i][j] = 0*4 + 3 + 1;
						break;
					case 6: //-1 = 1 + 4
						tile[i][j] = 1*4 + 3 + 1;
						break;
					case 7: //-1 = 2 + 4
						tile[i][j] = 0*4 + 0 + 1;
						break;
					case 8: //-1 = 1 + 2 + 4
						tile[i][j] = 1*4 + 0 + 1;
						break;
					case 9:
						tile[i][j] = 3*4 + 2 + 1;
						break;
					case 10: //-1 = 1 + 8
						tile[i][j] = 2*4 + 2 + 1;
						break;
					case 11: //-1 = 2 + 8
						tile[i][j] = 3*4 + 1 + 1;
						break;
					case 12: //-1 = 1 + 2 + 8
						tile[i][j] = 2*4 + 1 + 1;
						break;
					case 13: //-1 = 4 + 8
						tile[i][j] = 0*4 + 2 + 1;
						break;
					case 14:  //-1 = 1 + 4 + 8
						tile[i][j] = 1*4 + 2 + 1;
						break;
					case 15: //-1 = 2 + 4 + 8
						tile[i][j] = 0*4 + 1 + 1;
						break;
					case 16: //-1 = 1 + 2 + 4 + 8
						tile[i][j] = 1*4 + 1 + 1;
						break;	
				}
			}
		}
	}
	public int[] traceMap(double[][] hitbox, double deltax, double deltay, boolean isConsumer) { 
		//Top Left corner: ([0][0], [0][1])
		//Bottom Right corner: ([1][0], [1][1])
		double[] bottomLeft = {hitbox[0][0], hitbox[1][1]};
		double[] topRight = {hitbox[1][0], hitbox[0][1]};
		double[] middleUp = {(hitbox[0][0] + hitbox[1][0])/2, hitbox[0][1]}; 
		double[] middleRight = {hitbox[1][0], (hitbox[0][1] + hitbox[1][1])/2};  
		double[] middleBottom = {(hitbox[0][0] + hitbox[1][0])/2, hitbox[1][1]};
		double[] middleLeft = {hitbox[0][0], (hitbox[0][1] + hitbox[1][1])/2};
		double[][] corner = {hitbox[0], middleUp, topRight, middleRight, hitbox[1], middleBottom, bottomLeft, middleLeft};
		int[] nextTile = new int[8];
		for(int i = 0; i < 8; i++) {
			nextTile[i] = traceTile(corner[i], deltax, deltay, isConsumer);
		}
		return nextTile;
	}
	private int traceTile(double[] coor, double deltax, double deltay, boolean isConsumer) { 
		double[] deltaCoor = {coor[0] + deltax, coor[1] + deltay};
		int[] tilePos = {(int)deltaCoor[0]/Main.TILES_SIZE, (int)deltaCoor[1]/Main.TILES_SIZE};
		double[]  tileCoor = {tilePos[0]*Main.TILES_SIZE, tilePos[1]*Main.TILES_SIZE}; 
		if(tilePos[0] < 0 || tilePos[1] < 0 || 
				tilePos[0] >= SurvivalGame.WORLD_COL|| tilePos[1] > SurvivalGame.WORLD_ROW) {
			return 0;
		}
		int code = tile[tilePos[1]][tilePos[0]];
		 /*Check if point truly collide or not.*/
		boolean flag1 = (tileCoor[0] + imgCode[code].solidArea.x <= deltaCoor[0]);
		boolean flag2 = (deltaCoor[0] <= tileCoor[0] + imgCode[code].solidArea.x + imgCode[code].solidArea.width);
		boolean flag3 = (tileCoor[1] + imgCode[code].solidArea.y <= deltaCoor[1]);
		boolean flag4 = (deltaCoor[1] <= tileCoor[1] + imgCode[code].solidArea.y + imgCode[code].solidArea.height);
		if(!(flag1 && flag2 && flag3 && flag4)) 
			return 0;
		if(isConsumer && tileIsConsumable(code)) {
			replaceTileAt(tilePos[0], tilePos[1]);
		}
		return code;
	}
	private void replaceTileAt(int X, int Y) { /*column - row in matrix*/ 
		int code = tile[Y][X];
		visit.remove(Y*SurvivalGame.WORLD_COL + X);
		voidTile.add(Y*SurvivalGame.WORLD_COL + X);
		tile[Y][X] = 0;
		int rxc = voidTile.poll();
		tile[rxc/SurvivalGame.WORLD_COL][rxc%SurvivalGame.WORLD_COL] = code; 
	}
	public void render(Graphics2D g2) {
		for(int i = 0; i < SurvivalGame.WORLD_ROW; i++) {
			for(int j = 0; j < SurvivalGame.WORLD_COL; j++) {
				int code = tile[i][j];
				int worldX = j*Main.TILES_SIZE;
				int worldY = i*Main.TILES_SIZE;
				int screenX = worldX - SG.getPlayer().getWorldX() + SG.getPlayer().getScreenX(); 
				int screenY = worldY - SG.getPlayer().getWorldY() + SG.getPlayer().getScreenY();
				if(screenX >= -5*Main.TILES_SIZE && screenY >= -5*Main.TILES_SIZE 
					&& screenX <= Main.GAME_WIDTH + 5*Main.TILES_SIZE 
					&& screenY <= Main.GAME_HEIGHT + 5*Main.TILES_SIZE) {
					int sz = (int)((Main.TILES_DEFAULT_SIZE + PADD)*Main.SCALE);
					g2.drawImage(imgCode[code].image, screenX, screenY, sz, sz, null);
				}
			}
		}
	}
}

/*(Current) CODE 
0: Void
1 - 16: Block imgCodes
17: Single Stone
18: Trap
19: Orb
20: Decoration 1
21: Decoration 2
22: Decoration 3
99: Water
*/

