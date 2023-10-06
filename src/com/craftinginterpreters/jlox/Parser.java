/* 

递归下降分析——自顶向下解析器（实际优先级自低向高）
从最顶部或最外层的语法规则(这里是 program )开始，一直向下进入嵌套子表达式，最后到达语法树的叶子。
这与LR等自下而上的解析器从初级表达式(primary)开始，将其组成越来越大的语法块

print 在常规语言中应该只是库函数的一种，
此处为了能在实现定义和调用函数机制前使用 print 的功能，将其实现为语句。

program        → statement* EOF ;
statement      → exprStmt | printStmt ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ; */

package com.craftinginterpreters.jlox;

import java.util.List;
import java.util.ArrayList;

import static com.craftinginterpreters.jlox.TokenType.*;

class Parser {

    private static class ParseError extends RuntimeException {
    };

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {

        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd())
            statements.add(statement());
        return statements;
    }

    private Stmt statement() {
        if (match(PRINT))
            return printStatement();
        return expressionStatement();
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);

    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Expr expression() {
        return equality();
    }

    /*
     * 赋值语句
     * 因为 Binary 可以以任意方式嵌套，因此用 while 处理
     * equality，comparison，term,factor 实际上都是 Binary， 因此都用 while
     * unary 和 primary 用 if
     */
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /* 比较 */
    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /* 加减法 */
    private Expr term() {
        Expr expr = factor();
        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /* 乘除法 */
    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /* 单目运算符 */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE))
            return new Expr.Literal(false);

        if (match(TRUE))
            return new Expr.Literal(true);

        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING))
            return new Expr.Literal(previous().literal);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    /*
     * 检查下一个标记是否是预期的类型。
     * 如果是，它就会消费该标记，否则报告错误 （panic）
     */
    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        JLox.error(token, message);
        return new ParseError();
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;

    }

    // 返回当前 token 并移动 current 指向下一个 token
    private Token advance() {
        /*
         * Token ret = peek();
         * if (!isAtEnd())
         * current++;
         * return ret;
         */
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    // 返回当前 token
    private Token peek() {
        return tokens.get(current);
    }

    // 返回上一个消费的 token
    private Token previous() {
        return tokens.get(current - 1);
    }

}
