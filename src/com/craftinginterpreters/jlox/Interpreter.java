package com.craftinginterpreters.jlox;

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

class Interpreter implements Expr.Visitor<Object> {

    void interpreter(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringfy(value));
        } catch (RuntimeError error) {
            // 是 JLox 中的方法，而不是 RuntimeError 类
            JLox.runtimeError(error);
        }
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
