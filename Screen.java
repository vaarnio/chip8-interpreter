package com.company;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


public class Screen extends Canvas {
    private static final int width = 64;
    private static final int height = 32;

    private static int scale = 12;
    private GraphicsContext gc;
    private boolean[] gfx;


    public Screen(){
        super(width * scale, height * scale);
        gc  = this.getGraphicsContext2D();
        gfx = new boolean[64*32];

        clear();
    }

    public void draw(int x, int y, Color color){
        x = x * scale;
        y = y * scale;


        gc.setFill(color);
        gc.fillRect(x, y, 12, 12);
    }

    public void clear(){
        gc.setFill(Color.BLACK);
        gc.fillRect(0,0,800,400);
    }

    public void setGfx(boolean[] gfx) {
        this.gfx = gfx;
    }

    public boolean[] getGfx() {
        return gfx;
    }
}
