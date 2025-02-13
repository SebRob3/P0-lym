package com.p0lym;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

import javax.swing.text.StyledEditorKit.BoldAction;

import com.p0lym.RobotLexerParser.Token;
import com.p0lym.RobotLexerParser.TokenType;

public class Main {
    public static void main(String[] args) {
        String testProgram = "|nom x y one|\n" +
                "proc putChips: n andBalloons: m [\n" +
                "    |c b|\n" +
                "    c := n .\n" + 
                "    b := m .\n" + 
                "    put : c ofType: #chips .  put: b ofType: #balloons ]\n" +
                "\n" +
                "proc goNorth [\n" +
                "	 while: canMove: 1 inDir: #north do: [ move: 1 inDir: #north .\n" + 
                "        ]\n" + 
                "]\n" +
                "\n" +
                "\n" +
                "proc goWest [\n" + 
                "    if: canMove: 1 inDir: #west then: [move: 1 inDir: #west] else\n" + 
                "       : [nop .]]\n" +
                "\n" +
                "[\n" + 
                "    goTo: 3 with: 3 .\n" + 
                "    putChips: 2 andBalloons: 1 .\n" + 
                "]\n";

    	RobotLexerParser robot = new RobotLexerParser(new StringReader(testProgram));
    	ArrayList<Token> tokens = robot.lexer();
        Boolean correct = robot.parser(tokens);
    }
}