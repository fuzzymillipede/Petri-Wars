package petriwars.types;

import java.util.ArrayList;


public class Obstacle {
    private final byte OBSTACLE = 0x00;
    private ArrayList<Square> squares;
    private ArrayList<Square> corners;
    
    public Obstacle(byte[][] map_raw, Square[][] map, int x, int y)
    {
        squares = new ArrayList<Square>();
        corners = new ArrayList<Square>();
        grow(map_raw, map, x, y);
    }
    
    public void grow(byte[][] map_raw, Square[][] map, int x, int y)
    {
        squares.add(map[y][x]);
        map[y][x].addObstacle(this);
        int w = map[0].length;
        int h = map.length;
        int[] consec = new int[4]; //0: x - 1, 1: x + 1, 2: y - 1, 3: y + 1
        boolean[] dir = new boolean[2]; //0: up, 1: left
        if (isObstacle(x - 1, y - 1, w, h, map_raw, map))
        {
            consec[0]++;
            consec[2]++;
        }
        if (dir[0] = isObstacle(x, y - 1, w, h, map_raw, map))
        {
            consec[0]++;
            consec[1]++;
            consec[2]++;            
        }
        if (isObstacle(x + 1, y - 1, w, h, map_raw, map))
        {
            consec[1]++;
            consec[2]++;
        }
        
        if (dir[1] = isObstacle(x - 1, y, w, h, map_raw, map))
        {
            consec[0]++;
            consec[2]++;
            consec[3]++;
        }
        if (isObstacle(x + 1, y, w, h, map_raw, map))
        {
            consec[1]++;
            consec[2]++;
            consec[3]++;
        }
        
        if (isObstacle(x - 1, y + 1, w, h, map_raw, map))
        {
            consec[0]++;
            consec[3]++;
        }
        if (isObstacle(x, y + 1, w, h, map_raw, map))
        {
            consec[0]++;
            consec[1]++;
            consec[3]++;
        }
        if (isObstacle(x + 1, y + 1, w, h, map_raw, map))
        {
            consec[1]++;
            consec[3]++;
        }        
           
        if (consec[0] == 4)
        {
            if (dir[0]); //top left
            else; //bottom left
        }
        if (consec[1] == 4)
        {
            if (dir[0]); //top right
            else; //bottom right
        }
        if (consec[2] == 4)
        {
            if (dir[1]); //top left
            else; //top right
        }
        if (consec[3] == 4)
        {
            if (dir[1]); //bottom left
            else; //bottom right
        }       
    }
    
    private boolean isObstacle(int x, int y, int w, int h, byte[][] m, Square[][] m2)
    {
        if (x > 0 && y > 0 && x < w && y < h && m[y][x] == OBSTACLE)
        {
            grow(m, m2, x, y);
            return true;
        }
        return false;
    }
}