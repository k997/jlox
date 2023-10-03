package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static com.craftinginterpreters.jlox.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<Token>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // 保留关键字
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {

        while (!isAtEnd()) {
            start = current; // 每次扫描前更新start
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;
            // 双字运算符
            case '!':
                // advance()已经自动把current指向下一个字符
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                // 注释则跳过
                if (match('/')) {
                    while (peek() != '\n' && isAtEnd())
                        advance();
                } else { // 不是注释则添加 /
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    JLox.error(line, "Unexpected character.");
                }
                break;

        }
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    // 匹配失败返回false，成功则更新current并返回true
    private boolean match(char expected) {

        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;
        current++;
        return true;
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }
        // 代码结束未匹配到字符串的右引号
        if (isAtEnd()) {
            JLox.error(line, "Unterminated string.");
        }
        // 匹配到字符串的右引号并跳过右引号
        advance();

        // 去掉引号并添加 字符串 的实际值
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);

    }

    private void identifier() {
        while (isAlphaNumeric((peek())))
            advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null)
            type = IDENTIFIER;

        addToken(type);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void number() {
        // 跳过整数部分
        while (isDigit(peek()))
            advance();
        // 寻找是否有小数点，禁止 123. 格式
        if (peek() == '.' && isDigit((peekNext()))) {
            advance();
            // 跳过小数部分
            while (isDigit(peek()))
                advance();
        }
        // 将扫描到的数字转换为 java double
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char peek() {
        // 返回下一个字符, current 在 advance() 中已经 +1
        // 类似advance(),但不会消耗字符
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    // 下下个字符，但不会消费
    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }
}
