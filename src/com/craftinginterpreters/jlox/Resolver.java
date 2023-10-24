package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/* 
 * 块语句为它所包含的语句引入了一个新的作用域。
 * 函数声明为其函数体引入了一个新的作用域，并在该作用域中绑定了它的形参。
 * 变量声明将一个新变量加入到当前作用域中。
 * 变量定义和赋值表达式需要解析它们的变量值。
 */
class Resolver implements Expr.Visitor<Void>,
        Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    // 环境
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    // 防止在函数外调用 return
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    // 解析函数与类方法
    private void resolveFunction(Stmt.Function function, FunctionType functionType) {
        FunctionType enclosingFunction = currentFunction;
        // 借助 jvm 的栈使得在函数嵌套情况下保存函数类型
        currentFunction = functionType;
        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE)
            JLox.error(stmt.keyword, "Can't return from top-level code.");
        if (stmt.value != null)
            resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        // 解析右侧表达式
        resolve((expr.value));
        // 解析待赋值的变量
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)
            JLox.error(expr.name, "Can't read local variable in its own initializer.");
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // 变量的声明和定义被分为两步
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    // 变量声明
    void declare(Token name) {
        if (scopes.isEmpty())
            return;
        Map<String, Boolean> scope = scopes.peek();
        //
        /*
         * 同一作用域内禁止重复定义同名变量
         * 如
         * {
         * var a=1;
         * var a=2;
         * }
         */
        if (scope.containsKey(name.lexeme))
            JLox.error(name, "Already variable with this name in this scope.");
        scope.put(name.lexeme, false);
    }

    // 变量定义
    void define(Token name) {
        if (scopes.isEmpty())
            return;
        scopes.peek().put(name.lexeme, true);
    }

    // 分析多条语句
    void resolve(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            resolve(stmt);
        }
    }

    // 分析语句
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    // 分析表达式
    private void resolve(Expr expr) {
        expr.accept(this);
    }

    // 添加一个环境
    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    // 移除一个环境
    private void endScope() {
        scopes.pop();
    }
}