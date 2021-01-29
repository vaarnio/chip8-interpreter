package com.company;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static javafx.scene.paint.Color.WHITE;

public class Main extends Application {
    private Stage mainStage;

    private Screen screen;
    private Memory memory;
    private Keyboard keyboard;
    private Scene mainScene;
    BorderPane root;

    public void initialize(){
        //JavaFX gui initialization
        root = new BorderPane();
        mainScene = new Scene(root, 800, 400);
        screen = new Screen();  //extended from JavaFX Canvas
        root.getChildren().add(screen);
        mainStage.setScene(mainScene);

        //Emulation initialization
        keyboard = new Keyboard(mainScene);  //HEX keyboard responsible for key input
        memory = new Memory(screen, keyboard);  //memory, registers, opcode handling and other emulation stuff

        //add key pressed and released listeners for mainScene
        keyboard.onKeyPressed();
        keyboard.onKeyReleased();

        //screen.draw(10, 10, Color.BLUE);

        try {
            memory.loadProgram("./Maze.ch8");
            //memory.loadProgram("D:\\Downloads\\Windows\\CHIP-8-v.1.2-win64\\games\\roms\\REVERSI");
            //memory.loadProgram("D:\\Downloads\\Windows\\CHIP-8-v.1.2-win64\\games\\roms\\BC_test.ch8");
            //memory.loadProgram("D:\\Downloads\\Windows\\CHIP-8-v.1.2-win64\\games\\roms\\chip8-test-rom-master\\test_opcode.ch8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        new AnimationTimer(){
            public void handle(long l) {
                emulateCycle();
            }
        }.start();

        mainStage.show();
    }

    public void emulateCycle(){
        //Fetch opcode
        memory.fetchOpcode();

        //Decode opcode
        memory.decodeOpcode();

        //update timers
        memory.updateTimers();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        initialize();
    }
}
