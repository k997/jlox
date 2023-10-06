package com.craftinginterpreters.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class JLox {

    // 扫描和构建抽象语法树出现错误
    static boolean hadError = false;
    // 表达式求值出现错误
    static boolean hadRuntimeError = false;

    // 解释器——表达式求值
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes((Paths.get(path)));
        // Charset.defaultCharset()获取系统默认的字符编码，将字节数组转换为对应的字符串。
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError)
            System.exit(65);

        if (hadRuntimeError)
            System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null)
                break;
            run(line);
            hadError = false;

        }

    }

    private static void run(String source) {
        // 扫描词法单元
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // 构建抽象语法树
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        if (hadError)
            return;
        // 打印抽象语法树
        // System.out.println(new AstPrinter().print(expression));

        // 解释运行表达式
        interpreter.interpreter(expression);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF)
            report(token.line, "at end", message);
        else
            report(token.line, " at '" + token.lexeme + "'", message);
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

}