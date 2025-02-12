package com.p0lym;

import java.io.IOException;
import java.io.StringReader;

import com.p0lym.RobotLexer.Token;
import com.p0lym.RobotLexer.TokenType;

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

    	RobotLexer lexer = new RobotLexer(new StringReader(testProgram));
    	try {
    		Token token;
    		while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
    			System.out.println(token);
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
}