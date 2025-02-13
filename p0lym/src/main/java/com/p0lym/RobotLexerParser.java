package com.p0lym;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

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

    private ArrayList<Token> variables = new ArrayList<>();
    private ArrayList<Token> procedures = new ArrayList<>();
    
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
        if (Character.isLetter(ch) && Character.isLowerCase(ch)) {
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
    			tokens.add(token);
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}

        return tokens;
    }

    public boolean checkVariableDef(ArrayList<Token> tokens) {
        for (Token token : tokens) {
            if ((token.type == TokenType.IDENTIFIER) && (Character.isLowerCase(token.value.charAt(0))) ) {
                variables.add(token);
            }
        }

        return true;
    }

    //TODO checkProcSintaxis
    public boolean checkProcSintaxis(ArrayList<Token> tokens) {
        int tokenCount = 0;
        for (Token token : tokens) {
            if (tokenCount == 0) {
                if (token.getType() != TokenType.PROC) {
                    return false;
                }
                tokenCount++;
            } else if (tokenCount == 1) {
                if (token.getType() != TokenType.IDENTIFIER) {
                    return false;
                }
                procedures.add(token);
                tokenCount++;
            } else if (tokenCount == 2) {
                if (token.getType() != TokenType.COLON) {
                    return false;
                }
                tokenCount++;
            } else if (tokenCount == 3) {
                if (token.getType() != TokenType.IDENTIFIER) {
                    return false;
                }
                tokenCount++;
            } else if (tokenCount == 4) {
                if (token.getType() != TokenType.BRACKET_OPEN) {
                    return false;
                }
            } else if (tokenCount == 5) {
                if (token.getType() != TokenType.BRACKET_CLOSE) {
                    return false;
                }
            } else if (tokenCount == 6) {
                if (token.getType() != TokenType.PERIOD) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean parser(ArrayList<Token> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }

        int pipe = 0;

        ArrayList<Token> command = new ArrayList<>();
        for (Token token : tokens) {
            if ((token.getType() == TokenType.PIPE) && (pipe == 0)) {
                command.add(token);
                pipe++;
            } else if (pipe == 1) {
                command.add(token);
                if (token.getType() == TokenType.PIPE) {
                    pipe -= 1;
                    if (!checkVariableDef(tokens)) {
                        return false;
                    };
                    command.clear();
                };
            }
        }

        return true;
    }

}
