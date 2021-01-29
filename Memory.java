package com.company;

import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class Memory {
    private int opcode;                   //current opcode
    private int[] memory = new int[4096]; //memory
    private int[] V = new int[16];        //general purpose registers from V0 to Vf
    private int I;                        //index register I
    private int pc;                       //program counter

    private int[] stack = new int[16];    //remember current location before jump is performed, has 16 levels
    private int sp;                       //stack pointer which remembers which level of stack is being used

    private int delay_timer;              //when set above zero will count down to zero at 60hz
    private int sound_timer;              //when set above zero will count down to zero at 60hz, todo: buzzer sound at zero

    private Screen screen;                //JavaFX object for displaying the screen array
    private Keyboard keyboard;            //keyboard object for input

    public Memory(Screen screen, Keyboard keyboard){

        //initialize system
        pc = 0x200;  //Program counter starts at 0x200
        opcode = 0;  //Reset current opcode
        I = 0;       //Reset index register
        sp = 0;      //Reset stack pointer
        this.screen = screen;  //Clear display
        this.keyboard = keyboard;


        //Clear memory
        for(int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }

        // Reset stack and the V registers.
        for (int i = 0; i < 16; i++) {
            stack[i] = 0;
            V[i] = 0;
        }

        //Load fontset
        int[] chip8_fontset =
        {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        };
        for(int i = 0; i < chip8_fontset.length; i++){
            memory[i] = chip8_fontset[i];
        }

        //Reset timers
        delay_timer = 0;
        sound_timer = 0;
    }

    public void loadProgram(String pathString) throws IOException {
        Path path = Paths.get(pathString);
        byte[] program = Files.readAllBytes(path);
        int[] programUnsigned = new int[program.length];

        for(int i = 0; i < program.length; i++){
            programUnsigned[i] = program[i] & 0xFF;
            //System.out.println(programUnsigned[i]);
            memory[i + 512] = programUnsigned[i];  //Font data occupies the first 512 bytes of the memory space
        }
    }

    public void fetchOpcode(){
        opcode = memory[pc] << 8 | memory[pc + 1];  //shift first part 8 places and combine with OR
    }

    public void updateTimers(){
        if(delay_timer > 0){
            --delay_timer;
        }
        if(sound_timer > 0){
            if(sound_timer == 1) { System.out.println("BEEP!"); }  //
            --sound_timer;
        }
    }

    public void decodeOpcode() {  //these comments follow the same notation as opcode section in chip8 wikipedia page
        System.out.println("0x" + String.format("%x", opcode));
        switch (opcode & 0xF000) {
            case 0x0000:
                switch (opcode % 0x00FF){
                    case 0x00E0: //Clears the screen
                        screen.clear();
                        screen.setGfx(new boolean[64*32]);
                        pc += 2;
                        return;
                    case 0x00EE: //Returns from a subroutine
                        pc = stack[sp];
                        sp--;
                        pc += 2;
                        return;
                    default:
                        System.out.println("unknown opcode: " + opcode);
                }
            case 0x1000: //1NNN Jumps to address NNN
                pc = opcode & 0x0FFF;
                return;
            case 0x2000: //2NNN calls subroutine at NNN
                sp++;
                stack[sp] = pc;
                pc = opcode & 0x0FFF;
                return;
            case 0x3000: //3XNN Skips the next instruction if VX equals NN
                if(V[(opcode & 0x0F00) >>> 8] == (opcode & 0x00FF)){
                    pc += 4;
                    return;
                } else {
                    pc += 2;
                    return;
                }
            case 0x4000: //4XNN Skips the next instruction if VX doesn't equal NN
                if(V[(opcode & 0x0F00) >>> 8] != (opcode & 0x00FF)){
                    pc += 4;
                    return;
                } else {
                    pc += 2;
                    return;
                }
            case 0x5000: //5XY0 Skips the next instruction if VX equals VY
                if(V[(opcode & 0x0F00) >>> 8] == V[(opcode & 0x00F0) >>> 4]){
                    pc += 4;
                    return;
                } else {
                    pc += 2;
                    return;
                }
            case 0x6000: //6XNN Sets VX to NN
                V[(opcode & 0x0F00) >>> 8] = (opcode & 0x00FF);
                pc += 2;
                return;
            case 0x7000: //7XNN adds NN to VX
                if((V[(opcode & 0x0F00) >>> 8]) + (opcode & 0x00FF) >= 256){
                    V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x0F00) >>> 8] + (opcode & 0x00FF) - 256;
                } else {
                    V[(opcode & 0x0F00) >>> 8] += (opcode & 0x00FF);
                }
                //System.out.println("V" + ((opcode & 0x0F00) >>> 8) + " " +V[(opcode & 0x0F00) >>> 8]);
                pc += 2;
                return;
            case 0x8000:
                switch(opcode & 0x000F){
                    case 0x0000:  //8XY0 Set VX to the value of VY
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x00F0) >>> 4];
                        pc += 2;
                        return;
                    case 0x0001:  //8XY1 Sets VX to VX or VY (Bitwise OR operation)
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x0F00) >>> 8] | V[(opcode & 0x00F0) >>> 4];
                        pc += 2;
                        return;
                    case 0x0002:  //8XY2 Sets VX to VX and VY (Bitwise AND operation)
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x0F00) >>> 8] & V[(opcode & 0x00F0) >>> 4];
                        pc += 2;
                        return;
                    case 0x0003:  //8xy3 XOR Vx, Vy
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x0F00) >>> 8] ^ V[(opcode & 0x00F0) >>> 4];
                        pc += 2;
                        return;
                    case 0x0004:  //8XY4 Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
                        if(V[(opcode & 0x0F00) >>> 8] + V[(opcode & 0x00F0) >>> 4] > 255){
                            V[0xF] = 1;
                            //V[(opcode & 0x0F00) >>> 8] = 255;
                        } else {
                            V[0xF] = 0;
                            //V[(opcode & 0x0F00) >>> 8] += V[(opcode & 0x00F0) >>> 4];
                        }
                        V[(opcode & 0x0F00) >>> 8] = (V[(opcode & 0x0F00) >>> 8] + V[(opcode & 0x00F0) >>> 4]) & 0xFF;
                        pc += 2;
                        return;
                    case 0x0005:  //8XY5 If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted from Vx.
                        if(V[(opcode & 0x0F00) >>> 8] > V[(opcode & 0x00F0) >>> 4]){
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[(opcode & 0x0F00) >>> 8] = (V[(opcode & 0x0F00) >>> 8] - V[(opcode & 0x00F0) >>> 4]) & 0xFF;
                        pc += 2;
                        return;
                    case 0x0006:  //8xy6 If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2.
                        if((V[(opcode & 0x0F00) >>> 8] & 0x1) == 1){
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x0F00) >>> 8] >>> 1;
                        pc += 2;
                        return;
                    case 0x0007:  //8xy7 If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted from Vy, and the results stored in Vx.
                        //KESKEN
                        if(V[(opcode & 0x00F0) >>> 4] > V[(opcode & 0x0F00) >>> 8]){
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x00F0) >>> 4] - V[(opcode & 0x0F00) >>> 8];
                        pc += 2;
                        return;
                    case 0x000E:  //8xyE Set VF to the most significant bit of VX. Then Vx is multiplied by 2.
                        V[0xF] = V[(opcode & 0x0F00) >>> 8] >>> 7;
                        V[(opcode & 0x0F00) >>> 8] = V[(opcode & 0x0F00) >>> 8] * 2;
                        pc += 2;
                        return;
                    default:
                        System.out.println("unknown opcode: " + opcode);
                }
            case 0x9000:  //9XY0 Skips the next instruction if VX doesn't equal VY.
                if(V[(opcode & 0x0F00) >>> 8] != V[(opcode & 0x00F0) >>> 4]){
                    pc += 4;
                } else {
                    pc += 2;
                }
                return;
            case 0xA000: //ANNN Sets I to the address NNN.
                this.I = opcode & 0x0FFF;
                pc += 2;
                return;
            case 0xb000: //BNNN Jumps to the address NNN plus V0
                pc = (opcode & 0x0FFF) + V[0];
                return;
            case 0xC000: //CXNN Sets VX to the result of a bitwise and operation on a random number(typically 0-255) and NN
                int nn = opcode & 0x00FF;
                V[(opcode & 0x0F00) >>> 8] = new Random().nextInt(255) & nn;
                pc += 2;
                return;
            case 0xD000: {  //DXYN Draws a sprite at coordinate (VX, VY). Too long for a comment, read wiki.
                int pixel;
                int pixelIndex;
                Color color;

                for (int yline = 0; yline < (opcode & 0x000F); yline++){
                    pixel = memory[I + yline];

                    for(int xline = 0; xline < 8; xline++){
                        int pixelX = V[(opcode & 0x0F00) >>> 8] + xline;
                        int pixelY = V[(opcode & 0x00F0) >>> 4] + yline;

                        pixelIndex = (pixelX + ((pixelY) * 64))  % (64 * 32);  //remainder is used when sprite needs to wrap around

                        if((pixel & (0x80 >>> xline)) != 0){  //pixel of sprite is white
                            color = Color.WHITE;

                            if(!screen.getGfx()[pixelIndex]){ //pixel on display is black
                                screen.getGfx()[pixelIndex] = true;
                                screen.draw(pixelX, pixelY, color);
                                V[0xF] = 0;
                            }
                        } else {  //pixel of sprite is black
                            color = Color.BLACK;

                            if(screen.getGfx()[pixelIndex]){ //pixel on display is white
                                screen.getGfx()[pixelIndex] = false;
                                screen.draw(pixelX, pixelY, color);
                                V[0xF] = 1;
                            }
                        }
                    }
                }

                pc += 2;
                return;
            }
            case 0xE000:
                switch(opcode & 0x00FF) {
                    case 0x009E:  //EX9E Skips the nest instruction if key stored in VX is pressed
                        if(keyboard.getKeys()[V[(opcode & 0x0F00) >>> 8]]){
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        return;

                    case 0x00A1:  //EXA1 Skips the nest instruction if key stored in VX isn't pressed
                        if(!keyboard.getKeys()[V[(opcode & 0x0F00) >>> 8]]){
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        return;
                    default:
                        System.out.println("unknown opcode: " + opcode);
                }
            case 0xF000:
                switch(opcode & 0x00FF) {
                    case 0x0007:  //FX07 Sets VX to the value of the delay timer.
                        V[(opcode & 0x0F00) >>> 8] = delay_timer;
                        pc += 2;
                        return;
                    case 0x000A:  //FX0A a key press is awaited, and then stored in VX
                        if(keyboard.getPressed().isEmpty()){
                            return;
                        } else {
                            V[(opcode & 0x0F00) >>> 8] = keyboard.getPressed().get(0);
                            pc += 2;
                            return;
                        }
                    case 0x0015:  //FX15 Sets the delay timer to VX
                        delay_timer = V[(opcode & 0x0F00) >>> 8];
                        pc += 2;
                        return;
                    case 0x0018:  //FX18 Sets the sound timer to VX.
                        sound_timer = V[(opcode & 0x0F00) >>> 8];
                        pc += 2;
                        return;
                    case 0x001E:  //FX1E the values of I and Vx are added, and the results are stored in I.
                        if(I + V[(opcode & 0x0F00) >>> 8] > 0xFFF){
                            V[0xF] = 1;
                            I += V[(opcode & 0x0F00) >>> 8];
                        } else {
                            V[0xF] = 0;
                            I += V[(opcode & 0x0F00) >>> 8];
                        }
                        pc += 2;
                        return;
                    case 0x0029:  //FX29 Set I = location of sprite for digit Vx.
                        I = V[(opcode & 0x0F00) >>> 8] * 5;
                        pc += 2;
                        return;
                    case 0x0033:  //FX33 Store BCD representation of Vx in memory locations I, I+1, and I+2.
                        memory[I]     = (V[(opcode & 0x0F00) >>> 8] / 100);
                        memory[I + 1] = ((V[(opcode & 0x0F00) >>> 8] / 10) % 10);
                        memory[I + 2] = ((V[(opcode & 0x0F00) >>> 8] % 100) % 10);
                        pc += 2;
                        return;
                    case 0x0055:  //FX55 Store registers V0 through Vx in memory starting at location I.
                        for(int i = 0; i <= ((opcode & 0x0F00) >>> 8); i++){
                            memory[I + i] = V[i];
                        }
                        pc += 2;
                        return;
                    case 0x0065:  //FX65 Fills V0 to VX (including VX) with values from memory starting at address I.
                        for(int i = 0; i <= ((opcode & 0x0F00) >>> 8); i++){
                            V[i] = memory[I + i];
                        }
                        pc += 2;
                        return;
                    default:
                        System.out.println("unknown opcode: " + opcode);
                }
                return;
            //More opcodes

            default:
                System.out.println("unknown opcode: " + opcode);
        }
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public int[] getV() {
        return V;
    }
}
