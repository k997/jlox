package com.craftinginterpreters.jlox;

import java.util.List;

/* 

针对不同的类型，用访问者模式重写表达式求值方法


LOX 类在 JAVA 中的表示
| Lox type   Lox类 | Java representation   Java表示 |
|-----------------|------------------------------|
| Any Lox value   | Object                       |
| nil             | null                         |
| Boolean         | Boolean                      |
| number          | Double                       |
| string          | String                       |

 */

class Interpreter implements Expr.Visitor<Object>,
        Stmt.Visitor<Void> {

    private Environment environment = new Environment();

    void interpreter(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            // 是 JLox 中的方法，而不是 RuntimeError 类
            JLox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        // 如果该变量有初始化式，我们就对其求值。如果没有则初始化为 null
        if (stmt.initializer != null)
            value = evaluate(stmt.initializer);

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        // 执行语句中的表达式
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringfy(value));
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        // 赋值语句的值也是表达式
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        // 括号内也是表达式, 只需要递归地对子表达式求值并返回结果即可
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                // 对 Object 强制类型转换
                return -(double) right;
            case BANG:
                return !isTruthy(right);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;
                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right;

                throw new RuntimeError(expr.operator, "Operands must be numbers or strings");

            case MINUS:
                checkNumberOperand(expr.operator, right);
                return (double) left - (double) right;

            case SLASH:
                checkNumberOperand(expr.operator, right);
                return (double) left / (double) right;

            case STAR:
                checkNumberOperand(expr.operator, right);
                return (double) left * (double) right;

            case GREATER:
                checkNumberOperand(expr.operator, right);
                return (double) left > (double) right;

            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, right);
                return (double) left >= (double) right;

            case LESS:
                checkNumberOperand(expr.operator, right);
                return (double) left < (double) right;

            case LESS_EQUAL:
                checkNumberOperand(expr.operator, right);
                return (double) left <= (double) right;

            case BANG_EQUAL:
                return !isEqual(left, right);

            case EQUAL_EQUAL:
                return isEqual(left, right);

        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    // 对子表达式求值
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        // 确保调用 a.equals() 时 a 不为 null
        if (a == null)
            return false;
        return a.equals(b);
    }

    // false 和 nil 是假，其他都是真
    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    // 检查操作数是否为数字
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private String stringfy(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            // 如果是数字且数字最后为.0, 虽实际用 double 存储，但打印为整数
            String text = object.toString();
            if (text.endsWith(".0"))
                text = text.substring(0, text.length() - 2);
            return text;
        }
        return object.toString();
    }
}
