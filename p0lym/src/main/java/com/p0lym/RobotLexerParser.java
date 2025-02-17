package com.p0lym;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

public class RobotLexerParser {
    public static enum TokenType {
        // Symbols
        PIPE("|"),
        BRACKET_OPEN("["),
        BRACKET_CLOSE("]"),
        PERIOD("."),
        COLON(":"),
        ASSIGN(":="),
        
        // Keywords
        PROC("proc"),
        IF("if"),
        THEN("then"),
        ELSE("else"),
        WHILE("while"),
        DO("do"),
        FOR("for"),
        REPEAT("repeat"),

        // Other
        IDENTIFIER,
        NUMBER,
        CONSTANT, // Starting with #
        EOF, // End of file
        INVALID; // Invalid token
        
        private final String text;
        
        TokenType() {
            this.text = null;
        }
        
        TokenType(String text) {
            this.text = text;
        }
    }

    public static class Token {
        private TokenType type;
        private String value;
        private int line;
        private int column;
        
        public Token(TokenType type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }
        
        // Print in the console for test
        @Override
        public String toString() {
            return String.format("Token(%s, '%s', line=%d, col=%d)", 
                               type, value, line, column);
        }
        
        public TokenType getType() { 
            return type; 
        }
        public String getValue() {
             return value; 
        }
        public int getLine() { 
            return line; 
        }
        public int getColumn() { 
            return column; 
        }
    }

    private Reader reader;
    private int currentLine;
    private int currentColumn;
    private boolean pushBack; // it goes back a character. it enables to peek to the next char.
    private char lastChar;

    private HashMap<String, Token> variables = new HashMap<>();
    private HashMap<ArrayList<Token>, ArrayList<Token>> procedures = new HashMap<>();
    private HashMap<ArrayList<Token>, ArrayList<Token>> defaultProcedures = new HashMap<>();

    private HashMap<ArrayList<Token>, ArrayList<Token>> conditions = new HashMap<>();
    
    public RobotLexerParser(Reader input) {
        this.reader = new BufferedReader(input);
        this.currentLine = 1;
        this.currentColumn = 0;
        this.pushBack = false;
    }

    private char nextChar() throws IOException {
        if (pushBack) {
            pushBack = false;
            return lastChar;
        }
        
        int c = reader.read();
        if (c == -1) {
            return '\0';
        }
        
        lastChar = (char) c;
        currentColumn++;
        
        if (lastChar == '\n') {
            currentLine++;
            currentColumn = 0;
        }
        
        return lastChar;
    }

    private void pushBack() {
        pushBack = true;
    }

    public Token nextToken() throws IOException {
        // Skip whitespace
        char ch;
        do {
            ch = nextChar();
        } while (Character.isWhitespace(ch));
        
        // Token start position
        int tokenLine = currentLine;
        int tokenColumn = currentColumn;
        
        // Check for end of file
        if (ch == '\0') {
            return new Token(TokenType.EOF, "", tokenLine, tokenColumn);
        }
        
        // Identifiers and keywords
        if (Character.isLetter(ch)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(ch);
                ch = nextChar();
            } while (Character.isLetterOrDigit(ch) || ch == '_');
            pushBack();
            
            String word = sb.toString();
            
            // Check if it's a keyword
            for (TokenType type : TokenType.values()) {
                if (type.text != null && type.text.equals(word)) {
                    return new Token(type, word, tokenLine, tokenColumn);
                }
            }
            
            return new Token(TokenType.IDENTIFIER, word, tokenLine, tokenColumn);
        }
        
        // Numbers
        if (Character.isDigit(ch)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(ch);
                ch = nextChar();
            } while (Character.isDigit(ch));
            pushBack();
            
            return new Token(TokenType.NUMBER, sb.toString(), tokenLine, tokenColumn);
        }
        
        // Constants starting with #
        if (ch == '#') {
            StringBuilder sb = new StringBuilder();
            sb.append(ch);
            ch = nextChar();
            while (Character.isLetterOrDigit(ch)) {
                sb.append(ch);
                ch = nextChar();
            }
            pushBack();

            return new Token(TokenType.CONSTANT, sb.toString(), tokenLine, tokenColumn);
        }
        
        // Special symbols
        switch (ch) {
            case '|':
                return new Token(TokenType.PIPE, "|", tokenLine, tokenColumn);
            case '[':
                return new Token(TokenType.BRACKET_OPEN, "[", tokenLine, tokenColumn);
            case ']':
                return new Token(TokenType.BRACKET_CLOSE, "]", tokenLine, tokenColumn);
            case '.':
                return new Token(TokenType.PERIOD, ".", tokenLine, tokenColumn);
            case ':':
                ch = nextChar();
                if (ch == '=') {
                    return new Token(TokenType.ASSIGN, ":=", tokenLine, tokenColumn);
                }
                pushBack();
                return new Token(TokenType.COLON, ":", tokenLine, tokenColumn);
        }
        
        return new Token(TokenType.INVALID, Character.toString(ch), tokenLine, tokenColumn);
    }

    public ArrayList<Token> lexer() {
        ArrayList<Token> tokens = new ArrayList<>();
        try {
    		Token token;
    		while ((token = this.nextToken()).getType() != TokenType.EOF) {
    			if (token.getType() == TokenType.INVALID) {
                    System.out.println(token);
                    return new ArrayList<>();
                }
                tokens.add(token);
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}

        return tokens;
    }

    public Token variableType(Token token, HashMap<String, Token> localVariables) {
        Token variable = variables.get(token.value);
        if (variable == null) {
            variable = localVariables.get(token.value);
        }

        if (variable.getType() == TokenType.IDENTIFIER) {
            variable = variableType(variable, localVariables);
        }

        return variable;
    }

    public boolean existVariable(Token token, HashMap<String, Token> localVariables) {
        if (variables.containsKey(token.value)) {
            return true;
        } else if (localVariables.containsKey(token.value)) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<Token> checkVariableDef(ArrayList<Token> tokens) {
        int pipe = 0;
        ArrayList<Token> returnVariables = new ArrayList<>();
        for (Token token : tokens) {
            if ((token.type == TokenType.IDENTIFIER) && (Character.isLowerCase(token.value.charAt(0))) ) {
                returnVariables.add(token);
            } else if (token.type == TokenType.PIPE){
                pipe += 1;
            } else {
                System.out.println("Error: Variable def with wrong sintaxis");
                return new ArrayList<>();
            }
        }

        if (pipe != 2) {
            System.out.println("Error: Variable def with wrong sintaxis");
            return new ArrayList<>();
        }

        return returnVariables;
    }

    public ArrayList<Token> checkProcSintaxis(ArrayList<Token> tokens) {
        int tokenCount = 0;
        TokenType preLastToken = null;
        TokenType lastToken = null;

        ArrayList<Token> procName = new ArrayList<>();
        ArrayList<Token> procVariables = new ArrayList<>();

        for (Token token : tokens) {
            if (tokenCount == 0) {
                if (token.getType() != TokenType.PROC) {
                    System.out.println("Error: " + token);
                    return new ArrayList<>();
                }
                tokenCount++;
            } else if (tokenCount == 1) {
                if (((token.getType() != TokenType.IDENTIFIER) && (token.getValue()) != token.getValue().toLowerCase())) {
                    System.out.println("Error: " + token);
                    return new ArrayList<>();
                }
                procName.add(token);
                tokenCount++;
            } else if (tokenCount == 2) {
                if ((token.getType() != TokenType.COLON) && (tokens.size() != 3)) {
                    System.out.println("Error: " + token);
                    return new ArrayList<>();
                } else if (token.getType() == TokenType.COLON) {
                    preLastToken = TokenType.IDENTIFIER;
                    lastToken = token.getType();
                    tokenCount++;
                }
            } else if (tokenCount > 2) {
                if (lastToken == TokenType.COLON) {
                    if (token.getType() != TokenType.IDENTIFIER) {
                        System.out.println("Error: " + token);
                        return new ArrayList<>();
                    }
                    procVariables.add(token);
                    preLastToken = lastToken;
                    lastToken = token.getType();
                } else if (lastToken == TokenType.IDENTIFIER && preLastToken == TokenType.IDENTIFIER) {
                    if (token.getType() != TokenType.COLON) {
                        System.out.println("Error: " + token);
                        return new ArrayList<>();
                    }
                    preLastToken = lastToken;
                    lastToken = token.getType();
                } else if (lastToken == TokenType.IDENTIFIER && preLastToken == TokenType.COLON) {
                    if (token.getType() != TokenType.IDENTIFIER) {
                        System.out.println("Error: " + token);
                        return new ArrayList<>();
                    }
                    procName.add(token);
                    preLastToken = lastToken;
                    lastToken = token.getType();
                }
            }
        }

        procedures.put(procName, procVariables);

        return procName;
    }

    public boolean checkCallingDefProc(ArrayList<Token> tokens, HashMap<String, Token> localVariables) {
        // Ensure the tokens list has enough elements
        if (tokens.size() < 1) {
            System.out.println("Error: Procedure call with wrong syntax");
            return false;
        }
    
        // Ensure the last token is a PERIOD or BRACKET_CLOSE
        if (tokens.get(tokens.size() - 1).getType() != TokenType.PERIOD && tokens.get(tokens.size() - 1).getType() != TokenType.BRACKET_CLOSE) {
            System.out.println("Error: Procedure call with wrong syntax");
            return false;
        } else {
            tokens.remove(tokens.size() - 1);
        }
    
        HashMap<ArrayList<Token>, ArrayList<Token>> checkProcs = new HashMap<>();
    
        for (ArrayList<Token> procKey : defaultProcedures.keySet()) {
            if (procKey.get(0).getValue().equals(tokens.get(0).getValue())) {
                checkProcs.put(procKey, defaultProcedures.get(procKey));
            }
        }
    
        boolean callingProc = false;
        int key_i = 0;
        ArrayList<ArrayList<Token>> keys = new ArrayList<>(checkProcs.keySet());
        while (!callingProc && key_i < keys.size()) {
            ArrayList<Token> procName = keys.get(key_i);
            ArrayList<Token> procVariables = checkProcs.get(procName);
    
            int i = 0;
            int j = 0;
            int colon = 0;
            for (Token token : tokens) {
                if (i == j && token.getType() == TokenType.IDENTIFIER) {
                    if (i >= procName.size() || !procName.get(i).getValue().equals(token.getValue())) {
                        callingProc = false;
                        break;
                    }
                    i++;
                } else if (i > j && token.getType() == TokenType.COLON) {
                    colon++;
                } else if (i > j) {
                    if (j >= procVariables.size()) {
                        callingProc = false;
                        break;
                    }
                    Token variableRequest = procVariables.get(j);
                    if (variableRequest.getType() == TokenType.CONSTANT) {
                        boolean found = false;
                        for (Token variable : procVariables) {
                            if (token.getValue().equals(variable.getValue())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            callingProc = false;
                            break;
                        }
                    } else if (variableRequest.getType() == TokenType.NUMBER) {
                        if (token.getType() != TokenType.NUMBER && !existVariable(token, localVariables)) {
                            callingProc = false;
                            break;
                        }
                    }
                    j++;
                } else {
                    callingProc = false;
                    break;
                }
            }
    
            if (!(i == procName.size() && (colon == procName.size() || (colon == 0 && procName.size() == 1)))) {
                callingProc = false;
            } else {
                callingProc = true;
            }
            key_i++;
        }
    
        return callingProc;
    }

    public boolean checkCondition(ArrayList<Token> tokens, HashMap<String, Token> localVariables) {
        HashMap<ArrayList<Token>, ArrayList<Token>> checkConditions = new HashMap<>();

        if (tokens.get(0).value == "not") {
            if (tokens.get(1).getType() != TokenType.COLON) {
                System.out.println("Error: Condition with wrong sintaxis");
                return false;
            } else {
                tokens.remove(0);
                tokens.remove(1);
            }
        }

        for (ArrayList<Token> conditionKey : conditions.keySet()) {
            if (conditionKey.get(0).getValue().equals(tokens.get(0).getValue())) {
                checkConditions.put(conditionKey, conditions.get(conditionKey));
            }
        }

        boolean callingCondition = false;
        int key_i = 0;
        ArrayList<ArrayList<Token>> keys = new ArrayList<>(checkConditions.keySet());
        while (!callingCondition && key_i < keys.size()) {
            ArrayList<Token> conditionName = keys.get(key_i);
            ArrayList<Token> conditionVariables = checkConditions.get(conditionName);

            int i = 0;
            int j = 0;
            int colon = 0;
            for (Token token : tokens) {
                if (i == j && token.getType() == TokenType.IDENTIFIER) {
                    if (!conditionName.get(i).getValue().equals(token.getValue())) {
                        callingCondition = false;
                        break;
                    }
                    i++;
                } else if (i > j && token.getType() == TokenType.COLON){
                    colon++;
                } else if ( i > j) {
                    Token variableRequest = conditionVariables.get(j);
                    if (variableRequest.getType() == TokenType.CONSTANT) {
                        boolean found = false;
                        for (Token variable : conditionVariables) {
                            if (token.getValue().equals(variable.getValue())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            callingCondition = false;
                            break;
                        }
                    } else {
                        if (token.getType() != variableRequest.getType() && !existVariable(token, localVariables)) {
                            callingCondition = false;
                            break;
                        }
                    }
                    j++;
                } else {
                    callingCondition = false;
                    break;
                }
            }

            if (!(i == conditionName.size() && colon == conditionName.size())) {
                callingCondition = false;
            } else {
                callingCondition = true;
            }
            key_i++;
        }
        return callingCondition;
    }

    public boolean checkIf(ArrayList<Token> tokens, HashMap<String, Token> localVariables) {
        boolean ifStatement = false;
        boolean elseStatement = false;
        boolean bracket = false;

        ArrayList<Token> condition = new ArrayList<>();
        ArrayList<Token> codeBlockIf = new ArrayList<>();
        ArrayList<Token> codeBlockElse = new ArrayList<>();

        Token lastToken = null;

        for (Token token : tokens) {
            if (lastToken != null && (lastToken.getType() == TokenType.IF || lastToken.getType() == TokenType.THEN || lastToken.getType() == TokenType.ELSE) && token.getType() != TokenType.COLON) {
                System.out.println("Error: If statement with wrong sintaxis");
                return false;
            } else if (token.getType() == TokenType.IF) {
                ifStatement = true;
            } else if ((lastToken.getType() == TokenType.IF || lastToken.getType() == TokenType.THEN || lastToken.getType() == TokenType.ELSE)) {
                if (token.getType() != TokenType.COLON){
                    System.out.println("Error: If statement with wrong sintaxis");
                    return false;
                }
            }else if (ifStatement && (token.getType() != TokenType.THEN) && (token.getType() != TokenType.BRACKET_OPEN) && !bracket) {
                condition.add(token);
            } else if (ifStatement && (token.getType() == TokenType.THEN)) {
                if (!checkCondition(condition, localVariables)) {
                    System.out.println("Error: If statement with wrong sintaxis");
                    return false;
                }
            } else if (token.getType() == TokenType.ELSE) {
                elseStatement = true;
            } else if (elseStatement && (token.getType() == TokenType.BRACKET_OPEN)) {
                bracket = true;
            } else if (elseStatement && bracket && (token.getType() == TokenType.BRACKET_CLOSE)) {
                if (lastToken.type != TokenType.BRACKET_CLOSE && lastToken.type != TokenType.PERIOD) {
                    codeBlockElse.add(token);
                }
                bracket = false;
                elseStatement = false;
            } else if (ifStatement && (token.getType() == TokenType.BRACKET_OPEN)) {
                bracket = true;
            } else if (ifStatement && bracket && (token.getType() == TokenType.BRACKET_CLOSE)) {
                if (lastToken.type != TokenType.BRACKET_CLOSE && lastToken.type != TokenType.PERIOD) {
                    codeBlockIf.add(token);
                }
                bracket = false;
                ifStatement = false;
            } else if (ifStatement && bracket) {
                codeBlockIf.add(token);
            } else if (elseStatement && bracket) {
                codeBlockElse.add(token);
            } else {
                System.out.println("Error: If statement with wrong sintaxis");
                return false;
            }
            lastToken = token;
        }
        if (!parser(codeBlockIf, null, localVariables) || !parser(codeBlockElse, null, localVariables)) {
            return false;
        }

        return true;
    }

    public boolean checkWhile(ArrayList<Token> tokens, HashMap<String, Token> localVariables) {
        boolean whileStatement = false;
        boolean bracket = false;

        ArrayList<Token> condition = new ArrayList<>();
        ArrayList<Token> codeBlock = new ArrayList<>();

        Token lastToken = null;

        for (Token token : tokens) {
            if (lastToken != null && (lastToken.getType() == TokenType.WHILE || lastToken.getType() == TokenType.DO) && token.getType() != TokenType.COLON) {
                System.out.println("Error: While statement with wrong sintaxis");
                return false;
            } else if (token.getType() == TokenType.WHILE) {
                whileStatement = true;
            } else if (lastToken.type == TokenType.WHILE || lastToken.type == TokenType.DO) {
                if (token.getType() != TokenType.COLON) {
                    System.out.println("Error: While statement with wrong sintaxis");
                    return false;
                }
            } else if (whileStatement && (token.getType() != TokenType.DO && token.getType() != TokenType.BRACKET_OPEN)) {
                condition.add(token);
            } else if (whileStatement && (token.getType() == TokenType.DO)) {
                if (!checkCondition(condition, localVariables)) {
                    System.out.println("Error: While statement with wrong sintaxis");
                    return false;
                }
                whileStatement = false;
            } else if ((token.getType() == TokenType.BRACKET_OPEN)) {
                bracket = true;
                codeBlock.add(token);
            } else if (bracket && (token.getType() == TokenType.BRACKET_CLOSE)) {
                codeBlock.add(token);
                bracket = false;
                whileStatement = false;
            } else if (bracket) {
                codeBlock.add(token);
            } else {
                System.out.println("Error: While statement with wrong sintaxis");
                return false;
            }
            lastToken = token;
        }
        if (!parser(codeBlock, null, localVariables)) {
            return false;
        }
        else if (whileStatement || bracket) {
            System.out.println("Error: While statement with wrong sintaxis");
            return false;
        }

        return true;
    }

    public boolean checkFor(ArrayList<Token> tokens, HashMap<String, Token> localVariables) {
        boolean forStatement = false;
        boolean bracket = false;

        ArrayList<Token> codeBlock = new ArrayList<>();

        Token lastToken = null;

        for (Token token : tokens) {
            if (lastToken != null && (lastToken.getType() == TokenType.FOR || lastToken.getType() == TokenType.REPEAT) && token.getType() != TokenType.COLON) {
                System.out.println("Error: For statement with wrong sintaxis");
                return false;
            } else if (token.getType() == TokenType.FOR) {
                forStatement = true;
            } else if (forStatement && (token.getType() != TokenType.NUMBER && variableType(token, localVariables).getType() != TokenType.NUMBER)) {
                return false;
            } else if (forStatement && (token.getType() == TokenType.REPEAT)) {
                forStatement = false;
            } else if (forStatement && (token.getType() == TokenType.BRACKET_OPEN)) {
                bracket = true;
            } else if (forStatement && bracket && (token.getType() == TokenType.BRACKET_CLOSE)) {
                bracket = false;
                forStatement = false;
            } else if (bracket) {
                codeBlock.add(token);
            } else {
                System.out.println("Error: For statement with wrong sintaxis");
                return false;
            }
            lastToken = token;
        }
        if (forStatement || !parser(codeBlock, null, localVariables)) {
            return false;
        }

        return true;
    }

    public boolean checkCallingProc(ArrayList<Token> tokens, HashMap<String, Token> localVariables) {
        HashMap<ArrayList<Token>, ArrayList<Token>> checkProcs = new HashMap<>();

        if (tokens.get(tokens.size() - 1).getType() != TokenType.PERIOD && tokens.get(tokens.size() - 1).getType() != TokenType.BRACKET_CLOSE) {
            System.out.println("Error: Procedure call with wrong sintaxis");
            return false;
        } else {
            tokens.remove(tokens.size() - 1);
        }

        for (ArrayList<Token> procKey : procedures.keySet()) {
            if (procKey.get(0).getValue().equals(tokens.get(0).getValue())) {
                checkProcs.put(procKey, procedures.get(procKey));
            }
        }

        boolean callingProc = false;
        int key_i = 0;
        ArrayList<ArrayList<Token>> keys = new ArrayList<>(checkProcs.keySet());
        while (!callingProc && key_i < keys.size()) {
            ArrayList<Token> procName = keys.get(key_i);
            ArrayList<Token> procVariables = checkProcs.get(procName);

            int i = 0;
            int j = 0;
            int colon = 0;
            for (Token token : tokens) {
                if (i == j && token.getType() == TokenType.IDENTIFIER) {
                    if (!procName.get(i).getValue().equals(token.getValue())) {
                        callingProc = false;
                        break;
                    }
                    i++;
                } else if (i > j && token.getType() == TokenType.COLON){
                    colon++;
                }else if ( i > j) {
                    Token variableRequest = procVariables.get(j);
                    if (variableRequest.getType() == TokenType.IDENTIFIER) {
                        if (token.getType() == TokenType.IDENTIFIER && !localVariables.containsKey(token.value) && !variables.containsKey(token.value)) {
                            callingProc = false;
                            break;
                        }
                    } else {
                        if (token.getType() != variableRequest.getType() && variableRequest.getType() != variableType(token, localVariables).getType()) {
                            callingProc = false;
                            break;
                        }
                    }
                    j++;
                } else {
                    callingProc = false;
                    break;
                }
            }

            if (!(i == procName.size() && j == procVariables.size() && colon == procVariables.size())) {
                callingProc = false;
            } else {
                callingProc = true;
            }
            key_i++;
        }

        if (!callingProc) {
            tokens.add(new Token(TokenType.PERIOD, ".", 0, 0));
        }

        return callingProc;
    }

    private void defaultConditions() {
        // facing
        ArrayList<Token> facing = new ArrayList<>();
        facing.add(new Token(TokenType.IDENTIFIER, "facing", 0, 0));

        ArrayList<Token> facingVariables = new ArrayList<>();
        facingVariables.add(new Token(TokenType.CONSTANT, "#north", 0, 0));
        facingVariables.add(new Token(TokenType.CONSTANT, "#south", 0, 0));
        facingVariables.add(new Token(TokenType.CONSTANT, "#west", 0, 0));
        facingVariables.add(new Token(TokenType.CONSTANT, "#east", 0, 0));

        conditions.put(facing, facingVariables);

        // canPut
        ArrayList<Token> canPut = new ArrayList<>();
        canPut.add(new Token(TokenType.IDENTIFIER, "canPut", 0, 0));
        canPut.add(new Token(TokenType.IDENTIFIER, "ofType", 0, 2));

        ArrayList<Token> canPutVariables = new ArrayList<>();
        canPutVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        canPutVariables.add(new Token(TokenType.CONSTANT, "#ballons", 0, 0));
        canPutVariables.add(new Token(TokenType.CONSTANT, "#chips", 0, 0));

        conditions.put(canPut, canPutVariables);

        // canPick
        ArrayList<Token> canPick = new ArrayList<>();
        canPick.add(new Token(TokenType.IDENTIFIER, "canPick", 0, 0));
        canPick.add(new Token(TokenType.IDENTIFIER, "ofType", 0, 2));

        ArrayList<Token> canPickVariables = new ArrayList<>();
        canPickVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        canPickVariables.add(new Token(TokenType.CONSTANT, "#ballons", 0, 0));
        canPickVariables.add(new Token(TokenType.CONSTANT, "#chips", 0, 0));

        conditions.put(canPick, canPickVariables);

        // canMove
        ArrayList<Token> canMove = new ArrayList<>();
        canMove.add(new Token(TokenType.IDENTIFIER, "canMove", 0, 0));
        canMove.add(new Token(TokenType.IDENTIFIER, "inDir", 0, 2));

        ArrayList<Token> canMoveVariables = new ArrayList<>();
        canMoveVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        canMoveVariables.add(new Token(TokenType.CONSTANT, "#north", 0, 0));
        canMoveVariables.add(new Token(TokenType.CONSTANT, "#south", 0, 0));
        canMoveVariables.add(new Token(TokenType.CONSTANT, "#west", 0, 0));
        canMoveVariables.add(new Token(TokenType.CONSTANT, "#east", 0, 0));

        conditions.put(canMove, canMoveVariables);

        // canJump
        ArrayList<Token> canJump = new ArrayList<>();
        canJump.add(new Token(TokenType.IDENTIFIER, "canJump", 0, 0));
        canJump.add(new Token(TokenType.IDENTIFIER, "inDir", 0, 2));

        ArrayList<Token> canJumpVariables = new ArrayList<>();
        canJumpVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        canJumpVariables.add(new Token(TokenType.CONSTANT, "#north", 0, 0));
        canJumpVariables.add(new Token(TokenType.CONSTANT, "#south", 0, 0));
        canJumpVariables.add(new Token(TokenType.CONSTANT, "#west", 0, 0));
        canJumpVariables.add(new Token(TokenType.CONSTANT, "#east", 0, 0));

        conditions.put(canJump, canJumpVariables);

        // canMove toThe
        ArrayList<Token> canMoveToThe = new ArrayList<>();
        canMoveToThe.add(new Token(TokenType.IDENTIFIER, "canMove", 0, 0));
        canMoveToThe.add(new Token(TokenType.IDENTIFIER, "toThe", 0, 2));

        ArrayList<Token> canMoveToTheVariables = new ArrayList<>();
        canMoveToTheVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        canMoveToTheVariables.add(new Token(TokenType.CONSTANT, "#front", 0, 0));
        canMoveToTheVariables.add(new Token(TokenType.CONSTANT, "#back", 0, 0));
        canMoveToTheVariables.add(new Token(TokenType.CONSTANT, "#left", 0, 0));
        canMoveToTheVariables.add(new Token(TokenType.CONSTANT, "#right", 0, 0));

        conditions.put(canMoveToThe, canMoveToTheVariables);

        // canJump toThe
        ArrayList<Token> canJumpToThe = new ArrayList<>();
        canJumpToThe.add(new Token(TokenType.IDENTIFIER, "canJump", 0, 0));
        canJumpToThe.add(new Token(TokenType.IDENTIFIER, "toThe", 0, 2));

        ArrayList<Token> canJumpToTheVariables = new ArrayList<>();
        canJumpToTheVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        canJumpToTheVariables.add(new Token(TokenType.CONSTANT, "#front", 0, 0));
        canJumpToTheVariables.add(new Token(TokenType.CONSTANT, "#back", 0, 0));
        canJumpToTheVariables.add(new Token(TokenType.CONSTANT, "#left", 0, 0));
        canJumpToTheVariables.add(new Token(TokenType.CONSTANT, "#right", 0, 0));

        conditions.put(canJumpToThe, canJumpToTheVariables);

    }

    private void defaultProcs() {
        // Goto
        ArrayList<Token> goTo = new ArrayList<>();
        goTo.add(new Token(TokenType.IDENTIFIER, "goTo", 0, 0));
        goTo.add(new Token(TokenType.IDENTIFIER, "with", 0, 2));

        ArrayList<Token> goToVariables = new ArrayList<>();
        goToVariables.add(new Token(TokenType.NUMBER, "x", 0, 0));
        goToVariables.add(new Token(TokenType.NUMBER, "y", 0, 2));

        defaultProcedures.put(goTo, goToVariables);

        // Move
        ArrayList<Token> move = new ArrayList<>();
        move.add(new Token(TokenType.IDENTIFIER, "move", 0, 0));

        ArrayList<Token> moveVariables = new ArrayList<>();
        moveVariables.add(new Token(TokenType.NUMBER, "x", 0, 0));

        defaultProcedures.put(move, moveVariables);

        // Turn
        ArrayList<Token> turn = new ArrayList<>();
        turn.add(new Token(TokenType.IDENTIFIER, "turn", 0, 0));

        ArrayList<Token> turnVariables = new ArrayList<>();
        turnVariables.add(new Token(TokenType.CONSTANT, "#left", 0, 0));
        turnVariables.add(new Token(TokenType.CONSTANT, "#right", 0, 0));
        turnVariables.add(new Token(TokenType.CONSTANT, "#around", 0, 0));

        defaultProcedures.put(turn, turnVariables);

        // Face
        ArrayList<Token> face = new ArrayList<>();
        face.add(new Token(TokenType.IDENTIFIER, "face", 0, 0));

        ArrayList<Token> faceVariables = new ArrayList<>();
        faceVariables.add(new Token(TokenType.CONSTANT, "#north", 0, 0));
        faceVariables.add(new Token(TokenType.CONSTANT, "#south", 0, 0));
        faceVariables.add(new Token(TokenType.CONSTANT, "#west", 0, 0));
        faceVariables.add(new Token(TokenType.CONSTANT, "#east", 0, 0));

        defaultProcedures.put(face, faceVariables);

        // Put
        ArrayList<Token> put = new ArrayList<>();
        put.add(new Token(TokenType.IDENTIFIER, "put", 0, 0));
        put.add(new Token(TokenType.IDENTIFIER, "ofType", 0, 2));

        ArrayList<Token> putVariables = new ArrayList<>();
        putVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        putVariables.add(new Token(TokenType.CONSTANT, "#ballons", 0, 2));
        putVariables.add(new Token(TokenType.CONSTANT, "#chips", 0, 2));

        defaultProcedures.put(put, putVariables);

        // Pick
        ArrayList<Token> pick = new ArrayList<>();
        pick.add(new Token(TokenType.IDENTIFIER, "pick", 0, 0));
        pick.add(new Token(TokenType.IDENTIFIER, "ofType", 0, 2));

        ArrayList<Token> pickVariables = new ArrayList<>();
        pickVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        pickVariables.add(new Token(TokenType.CONSTANT, "#ballons", 0, 2));
        pickVariables.add(new Token(TokenType.CONSTANT, "#chips", 0, 2));

        defaultProcedures.put(pick, pickVariables);

        // move tothe
        ArrayList<Token> moveToThe = new ArrayList<>();
        moveToThe.add(new Token(TokenType.IDENTIFIER, "move", 0, 0));
        moveToThe.add(new Token(TokenType.IDENTIFIER, "toThe", 0, 2));

        ArrayList<Token> moveToTheVariables = new ArrayList<>();
        moveToTheVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        moveToTheVariables.add(new Token(TokenType.CONSTANT, "#front", 0, 2));
        moveToTheVariables.add(new Token(TokenType.CONSTANT, "#back", 0, 2));
        moveToTheVariables.add(new Token(TokenType.CONSTANT, "#left", 0, 2));
        moveToTheVariables.add(new Token(TokenType.CONSTANT, "#right", 0, 2));

        defaultProcedures.put(moveToThe, moveToTheVariables);

        // move indir
        ArrayList<Token> moveIndir = new ArrayList<>();
        moveIndir.add(new Token(TokenType.IDENTIFIER, "move", 0, 0));
        moveIndir.add(new Token(TokenType.IDENTIFIER, "inDir", 0, 2));

        ArrayList<Token> moveIndirVariables = new ArrayList<>();
        moveIndirVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        moveIndirVariables.add(new Token(TokenType.CONSTANT, "#north", 0, 2));
        moveIndirVariables.add(new Token(TokenType.CONSTANT, "#south", 0, 2));
        moveIndirVariables.add(new Token(TokenType.CONSTANT, "#west", 0, 2));
        moveIndirVariables.add(new Token(TokenType.CONSTANT, "#east", 0, 2));

        defaultProcedures.put(moveIndir, moveIndirVariables);

        // jump toThe
        ArrayList<Token> jumpToThe = new ArrayList<>();
        jumpToThe.add(new Token(TokenType.IDENTIFIER, "jump", 0, 0));
        jumpToThe.add(new Token(TokenType.IDENTIFIER, "toThe", 0, 2));

        ArrayList<Token> jumpToTheVariables = new ArrayList<>();
        jumpToTheVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        jumpToTheVariables.add(new Token(TokenType.CONSTANT, "#front", 0, 2));
        jumpToTheVariables.add(new Token(TokenType.CONSTANT, "#back", 0, 2));
        jumpToTheVariables.add(new Token(TokenType.CONSTANT, "#left", 0, 2));
        jumpToTheVariables.add(new Token(TokenType.CONSTANT, "#right", 0, 2));

        defaultProcedures.put(jumpToThe, jumpToTheVariables);

        // jump indir
        ArrayList<Token> jumpIndir = new ArrayList<>();
        jumpIndir.add(new Token(TokenType.IDENTIFIER, "jump", 0, 0));
        jumpIndir.add(new Token(TokenType.IDENTIFIER, "inDir", 0, 2));

        ArrayList<Token> jumpIndirVariables = new ArrayList<>();
        jumpIndirVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));
        jumpIndirVariables.add(new Token(TokenType.CONSTANT, "#north", 0, 2));
        jumpIndirVariables.add(new Token(TokenType.CONSTANT, "#south", 0, 2));
        jumpIndirVariables.add(new Token(TokenType.CONSTANT, "#west", 0, 2));
        jumpIndirVariables.add(new Token(TokenType.CONSTANT, "#east", 0, 2));

        defaultProcedures.put(jumpIndir, jumpIndirVariables);

        // nop
        ArrayList<Token> nop = new ArrayList<>();
        nop.add(new Token(TokenType.IDENTIFIER, "nop", 0, 0));

        ArrayList<Token> nopVariables = new ArrayList<>();

        defaultProcedures.put(nop, nopVariables);

        // M
        ArrayList<Token> m = new ArrayList<>();
        m.add(new Token(TokenType.IDENTIFIER, "M", 0, 0));

        ArrayList<Token> mVariables = new ArrayList<>();

        defaultProcedures.put(m, mVariables);

        // R
        ArrayList<Token> r = new ArrayList<>();
        r.add(new Token(TokenType.IDENTIFIER, "R", 0, 0));

        ArrayList<Token> rVariables = new ArrayList<>();

        defaultProcedures.put(r, rVariables);

        // C
        ArrayList<Token> c = new ArrayList<>();
        c.add(new Token(TokenType.IDENTIFIER, "C", 0, 0));

        ArrayList<Token> cVariables = new ArrayList<>();

        defaultProcedures.put(c, cVariables);

        // B
        ArrayList<Token> b = new ArrayList<>();
        b.add(new Token(TokenType.IDENTIFIER, "B", 0, 0));

        ArrayList<Token> bVariables = new ArrayList<>();

        defaultProcedures.put(b, bVariables);

        // c
        ArrayList<Token> c2 = new ArrayList<>();
        c2.add(new Token(TokenType.IDENTIFIER, "c", 0, 0));

        ArrayList<Token> c2Variables = new ArrayList<>();

        defaultProcedures.put(c2, c2Variables);

        // b

        ArrayList<Token> b2 = new ArrayList<>();

        b2.add(new Token(TokenType.IDENTIFIER, "b", 0, 0));

        ArrayList<Token> b2Variables = new ArrayList<>();

        defaultProcedures.put(b2, b2Variables);

        // P
        ArrayList<Token> p = new ArrayList<>();

        p.add(new Token(TokenType.IDENTIFIER, "P", 0, 0));

        ArrayList<Token> pVariables = new ArrayList<>();

        defaultProcedures.put(p, pVariables);

        //J(n)
        ArrayList<Token> j = new ArrayList<>();
        j.add(new Token(TokenType.IDENTIFIER, "J(", 0, 0));
        j.add(new Token(TokenType.IDENTIFIER, ")", 0, 0));

        ArrayList<Token> jVariables = new ArrayList<>();
        jVariables.add(new Token(TokenType.NUMBER, "n", 0, 0));

        defaultProcedures.put(j, jVariables);

        //G(x,y)
        ArrayList<Token> g = new ArrayList<>();
        g.add(new Token(TokenType.IDENTIFIER, "G(", 0, 0));
        g.add(new Token(TokenType.IDENTIFIER, ",", 0, 0));
        g.add(new Token(TokenType.IDENTIFIER, ")", 0, 0));

        ArrayList<Token> gVariables = new ArrayList<>();
        gVariables.add(new Token(TokenType.NUMBER, "x", 0, 0));
        gVariables.add(new Token(TokenType.NUMBER, "y", 0, 0));

        defaultProcedures.put(g, gVariables);
    }

    public boolean parser(ArrayList<Token> tokens, ArrayList<Token> procToken, HashMap<String, Token> localVariables) {
        
        if (localVariables == null) {
            localVariables = new HashMap<>();
        }
        if (tokens.isEmpty()) {
            System.out.println("Error: Invalid token");
            return false;
        }
        if (procToken != null) {
            boolean procFound = false;
            for (ArrayList<Token> procKey : defaultProcedures.keySet()) {
                if (procKey.get(0).getValue().equals(procToken.get(0).getValue())) {
                    for (Token variable : defaultProcedures.get(procKey)) {
                        localVariables.put(variable.getValue(), null);
                    }
                    procFound = true;
                    break;
                }
            }
            if (!procFound) {
                for (ArrayList<Token> procKey : procedures.keySet()) {
                    if (procKey.get(0).getValue().equals(procToken.get(0).getValue())) {
                        for (Token variable : procedures.get(procKey)) {
                            localVariables.put(variable.getValue(), null);
                        }
                        procFound = true;
                        break;
                    }
                }
            }
        }
        
        if (defaultProcedures.isEmpty()) {
            this.defaultProcs();
        }
        if (conditions.isEmpty()) {
            this.defaultConditions();
        }

        int pipe = 0;

        boolean callingProc = false;
        boolean foundProc = false;
        boolean proc = false;
        ArrayList<Token> procName = null;
        int bracket = 0;

        boolean ifStatement = false;
        boolean elseStatement = false;

        boolean whileStatement = false;

        boolean forStatement = false;

        Token variableName = null;
        boolean variable = false;

        Token lastToken = null;

        ArrayList<Token> command = new ArrayList<>();
        for (Token token : tokens) {
            if (lastToken != null && token.getType() == TokenType.BRACKET_CLOSE && tokens.get(tokens.size() - 1) == token && lastToken.getType() == TokenType.PERIOD && !proc && !callingProc && bracket == 0) {
                break;
            }
            //Check Variable definitions
            else if ((token.getType() == TokenType.PIPE) && (pipe == 0) && bracket == 0) {
                command.add(token);
                pipe++;
            } else if (pipe == 1) {
                command.add(token);
                if (token.getType() == TokenType.PIPE) {
                    pipe -= 1;
                    ArrayList<Token> defVariables = checkVariableDef(command);
                    if (defVariables.isEmpty()) {
                        System.out.println("Error: Procedure definition with wrong sintaxis");
                        return false;
                    }
                    for (Token defVariable : defVariables) {
                        if (procToken != null) {
                            localVariables.put(defVariable.value, null);
                        } else if (procToken == null) {
                            variables.put(defVariable.value, null);
                        }
                    }
                    command.clear();
                };
            } 
            
            //Check Procedures
            else if (token.getType() == TokenType.PROC) {
                proc = true;
                command.add(token);
            } else if (proc && (token.getType() != TokenType.BRACKET_OPEN && token.getType() != TokenType.BRACKET_CLOSE && bracket == 0)) {
                command.add(token);
            } else if (proc && (token.getType() == TokenType.BRACKET_OPEN) && bracket == 0) {
                procName = checkProcSintaxis(command);
                if (procName.isEmpty()) {
                    return false;
                }
                command.clear();
                bracket++;
                proc = true;
            } else if (bracket > 0 && proc && (token.getType() == TokenType.BRACKET_OPEN)) {
                bracket++;
                command.add(token);
            }else if (bracket > 0 && proc && (token.getType() != TokenType.BRACKET_CLOSE)) {
                command.add(token);
            } else if (bracket > 0 && proc && (token.getType() == TokenType.BRACKET_CLOSE)) {
                
                if (bracket == 1) {
                    if (command.get(command.size() - 1).getType() != TokenType.PERIOD && command.get(command.size() - 1).getType() != TokenType.BRACKET_CLOSE) {
                        command.add(token);
                    }
                    if (!parser(command, procName, localVariables)) {
                        return false;
                    }
                    command.clear();
                    proc = false;
                    procName = null;
                } else if (bracket > 1) {
                    command.add(token);
                }
                bracket--;
            }

            //Check if
            else if (token.getType() == TokenType.IF) {
                command.add(token);
                ifStatement = true;
            } else if (token.getType() == TokenType.ELSE) {
                command.add(token);
                elseStatement = true;
            } else if (ifStatement && elseStatement && (token.getType() == TokenType.BRACKET_OPEN)) {
                bracket++;
                command.add(token);
            } else if (ifStatement && elseStatement && (token.getType() == TokenType.BRACKET_CLOSE)) {
                command.add(token);
                if (!checkIf(command, localVariables)) {
                    return false;
                }
                command.clear();
                ifStatement = false;
                elseStatement = false;
                bracket--;
            } else if (ifStatement && (token.getType() != TokenType.ELSE)) {
                command.add(token);
            }

            //Check while
            else if (token.getType() == TokenType.WHILE && !proc) {
                command.add(token);
                whileStatement = true;
            } else if (whileStatement && (token.getType() == TokenType.BRACKET_OPEN) && !proc) {
                bracket++;
                command.add(token);
            } else if (whileStatement && bracket > 0 && (token.getType() == TokenType.BRACKET_CLOSE) && !proc) {
                command.add(token);
                if (!checkWhile(command, localVariables)) {
                    return false;
                }
                command.clear();
                whileStatement = false;
                bracket--;
            } else if (whileStatement && !proc) {
                command.add(token);
            }

            //Check for
            else if (token.getType() == TokenType.FOR) {
                command.add(token);
                forStatement = true;
            } else if (forStatement && (token.getType() == TokenType.BRACKET_OPEN)) {
                bracket++;
                command.add(token);
            } else if (forStatement && bracket > 0 && (token.getType() == TokenType.BRACKET_CLOSE)) {
                command.add(token);
                if (!checkFor(command, localVariables)) {
                    return false;
                }
                command.clear();
                forStatement = false;
                bracket--;
            } else if (forStatement) {
                command.add(token);
            }

            //Check Code Block
            else if (token.getType() == TokenType.BRACKET_OPEN) {
                bracket++;
            } else if (token.getType() == TokenType.BRACKET_CLOSE && !callingProc) {
                if (tokens.get(tokens.size() - 2).getType() != TokenType.PERIOD) {
                    command.add(token);
                }
                if (!parser(command, procName, localVariables)) {
                    return false;
                }
                command.clear();
                bracket--;
            } else if (bracket > 0) {
                command.add(token);
            }

            //Check Variables
            else if ((token.getType() == TokenType.ASSIGN)) {
                if (lastToken == null || lastToken.getType() != TokenType.IDENTIFIER) {
                    System.out.println("Error: Variable assignment with wrong sintaxis (" + lastToken + ")");
                    return false;
                }
                boolean definedVariable = false;
                for (String variableKey : localVariables.keySet()) {
                    if (variableKey.equals(lastToken.getValue())) {
                        definedVariable = true;
                        break;
                    }
                }
                if (!definedVariable) {
                    for (String variableKey : variables.keySet()) {
                        if (variableKey.equals(lastToken.getValue())) {
                            definedVariable = true;
                            break;
                        }
                    }
                }
                if (!definedVariable) {
                    System.out.println("Error: Variable used but never defined (" + lastToken + ")");
                    return false;
                }
                command.add(lastToken);
                command.add(token);

                variableName = lastToken;
                callingProc = false;
                variable = true;
            } else if (variable && (token.getType() == TokenType.NUMBER || token.getType() == TokenType.CONSTANT || token.getType() == TokenType.IDENTIFIER)) {
                if (token.getType() == TokenType.IDENTIFIER) {
                    boolean existingVariable = false;
                    for (String variableKey : localVariables.keySet()) {
                        if (variableKey.equals(token.getValue())) {
                            existingVariable = true;
                            break;
                        }
                    }
                    if (!existingVariable) {
                        for (String variableKey : variables.keySet()) {
                            if (variableKey.equals(token.getValue())) {
                                existingVariable = true;
                                break;
                            }
                        }
                    }
                    if (!existingVariable) {
                        System.out.println("Error: Variable assignment with wrong sintaxis (" + token + ")");
                        return false;
                    } else if (procToken != null) {
                        localVariables.put(variableName.value, token);
                    } else if (procToken == null) {
                        variables.put(variableName.value, token);
                    }
                }
                command.clear();
            } else if (token.getType() == TokenType.PERIOD && variable) {
                command.clear();
                variable = false;
            }

            else {
                //Check procedures calls
                if (!callingProc) {
                    for (ArrayList<Token> procKey : procedures.keySet()) {
                        if (procKey.get(0).getValue().equals(token.getValue())) {
                            callingProc = true;
                            foundProc = false;
                            break;
                        }
                    }
                }

                if (callingProc && (token.getType() != TokenType.PERIOD) && (token.getType() != TokenType.BRACKET_CLOSE)) {
                    command.add(token);
                } else if (callingProc && (token.getType() == TokenType.PERIOD || token.getType() == TokenType.BRACKET_CLOSE)) {
                    command.add(token);
                    if (checkCallingProc(command, localVariables)) {
                        command.clear();
                        callingProc = false;
                        foundProc = true;
                    }
                }
                
                if (!callingProc && !foundProc) {
                    for (ArrayList<Token> procKey : defaultProcedures.keySet()) {
                        if (procKey.get(0).getValue().equals(token.getValue())) {
                            command.add(token);
                            callingProc = true;
                            foundProc = false;
                            break;
                        }
                    }
                }

                if (callingProc && (token.getType() == TokenType.PERIOD || token.getType() == TokenType.BRACKET_CLOSE) && !foundProc) {
                    if (!checkCallingDefProc(command, localVariables)) {
                        return false;
                    }
                    command.clear();
                    callingProc = false;
                }
            }
            lastToken = token;
        }

        if ((pipe != 0) || (callingProc != false) || (proc != false) || (ifStatement != false) || (elseStatement != false) || (whileStatement != false) || (forStatement != false) || (bracket != 0) || (variable != false)) {
            return false;
        }

        return true;
    }
}
