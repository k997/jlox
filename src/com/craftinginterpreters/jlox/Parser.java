/* 

递归下降分析——自顶向下解析器（实际优先级自低向高）
从最顶部或最外层的语法规则(这里是 program )开始，一直向下进入嵌套子表达式，最后到达语法树的叶子。
这与LR等自下而上的解析器从初级表达式(primary)开始，将其组成越来越大的语法块

print 在常规语言中应该只是库函数的一种，
此处为了能在实现定义和调用函数机制前使用 print 的功能，将其实现为语句。

for 语句只是 while 语句的语法糖


可以把调用看作是一种以`(`开头的后缀运算符, 调用 = primary + ()
被调用函数的名称实际上并不是调用语法的一部分,

funDecl 和 function 分开是因为类方法也会复用 function

类中的 getter 方法复用 "."，setter 方法复用 "="

Lox是动态类型的，所以没有真正的void函数。
省略了`return `语句中的值，我们将其视为等价于`return nil;`

this 语句的实现与闭包的机制相同

program        → declaration* EOF;
declaration    → classDecl | varDecl | funDecl |statement ;
classDecl      → "class" IDENTIFIER "{" function* "}" ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
statement      → exprStmt
               | ifStmt
               | printStmt
               | whileStmt
               | forStmt
               | block 
               | returnStmt ;
returnStmt     → "return" expression? ";" ;
whileStmt      → "while" "(" expression ")" statement ;
forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;
ifStmt         → "if" "(" expression ")" statement
               ( "else" statement )? ;
block          → "{" declaration* "}" ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
expression     → assignment ;
assignment     → (call "." )? IDENTIFIER "=" assignment
               | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | call ;
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
arguments      → expression ( "," expression )* ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")"
               | THIS
               | IDENTIFIER ; */

package com.craftinginterpreters.jlox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static com.craftinginterpreters.jlox.TokenType.*;

class Parser {

    private static class ParseError extends RuntimeException {
    };

    private final List<Token> tokens;
    // 函数调用支持的最大参数数量
    private final int MAX_ARITY = 255;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {

        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd())
            statements.add(declaration());
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(CLASS))
                return classDeclaration();
            if (match(VAR))
                return varDeclaration();
            if (match(FUN))
                return function("function");
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd())
            methods.add(function("method"));

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL))
            initializer = expression();
        consume(SEMICOLON, "Expect ':' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt.Function function(String kind) {
        // 解析函数名/方法名
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '()' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= MAX_ARITY)
                    error(peek(), "Can't have more than " + MAX_ARITY + " parameters.");
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");

        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    private Stmt statement() {
        if (match(PRINT))
            return printStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());
        if (match(IF))
            return ifStatement();
        if (match(WHILE))
            return whileStatement();
        if (match(FOR))
            return forStatement();
        if (match(RETURN))
            return returnStatement();
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

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // 解析初始化语句
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // 解析循环条件表达式
        Expr condition = null;
        // loop condition 不是空语句则解析表达式，否则恒为 true
        if (!check(SEMICOLON))
            condition = expression();
        // 条件表达式为空则恒为 true
        if (condition == null)
            condition = new Expr.Literal(true);
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // 解析递增表达式
        Expr increment = null;
        if (!check(RIGHT_PAREN))
            increment = expression();

        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // 解析循环体
        Stmt body = statement();
        /*
         * // for 循环中 (var i = 0; i < 10; i = i + 1) 各部分都可以省略
         * 
         * // for 循环
         * for (var i = 0; i < 10; i = i + 1) print i;
         * // 对应的等价 while 形式
         * {
         * var i = 0;
         * while (i < 10) {
         * print i;
         * i = i + 1;
         * }
         * }
         */

        // 如果有递增表达式，则将其和循环体打包到一个 block 中
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body, new Stmt.Expression(increment)));
        }

        // 加增量表达和循环体打包为 while 循环语句
        body = new Stmt.While(condition, body);

        // 有初始化语句则将初始化语句和 while 循环语句打包成 block
        // 该 block 即 for 语句的 block
        if (initializer != null)
            body = new Stmt.Block(Arrays.asList(initializer, body));

        return body;
    }

    private Stmt returnStatement() {
        // keyword 即被消耗的 `RETURN`
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON))
            value = expression();
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    // 赋值语句
    private Expr assignment() {
        // 类似二元表达式，先解析左侧表达式
        Expr expr = or();
        // 左侧表达式解析完后如果是 '=' ，说明是赋值语句
        if (match(EQUAL)) {
            // 检测左侧表达式的结果是否是合法的变量
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                // 计算右侧表达式
                Expr value = assignment();
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                Expr value = assignment();
                return new Expr.Set(get.object, get.name, value);
            }
            // 保留 '=' 的 token 用于出现错误时错误处理
            Token equals = previous();
            error(equals, "Invalid assignment targe.");

        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            // operator 即 OR
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            // operator 即 AND
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /*
     * 相等性（相等/不等）：==,!=
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

    /* 比较大小：>, >=, <, <= */
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
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        // 函数可能被嵌套调用
        while (true) {
            if (match(LEFT_PAREN))
                expr = finishCall(expr);
            else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'");
                expr = new Expr.Get(expr, name);
            } else
                break;
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            // 最大支持的参数
            if (arguments.size() >= MAX_ARITY)
                error(peek(), "can't hava more than " + MAX_ARITY + "arguments.");
            // 不是右括号，说明有一个以上参数
            do {
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
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

        if (match(IDENTIFIER))
            return new Expr.Variable(previous());

        if (match(THIS))
            return new Expr.This(previous());

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

    // ParseError后，，调用该方法不断丢弃标记，直到它发现一个语句的边界，以期望回到同步状态
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            // 消耗掉的 token 是分号
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }
            advance();
        }
    }
}
