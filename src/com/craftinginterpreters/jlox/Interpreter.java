package com.craftinginterpreters.jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    final Environment globals = new Environment();
    // environment 会随着作用域改变而变化
    private Environment environment = globals;
    // 语义分析时记录局部变量所处的环境的层级
    private final Map<Expr, Integer> locals = new HashMap<>();

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

    // 语义分析时记录局部变量所处的环境的层级
    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        JLoxFunction function = new JLoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        // 有返回值则对返回值求值
        if (stmt.value != null)
            value = evaluate(stmt.value);
        // JLox 的 return 使用 Java 的异常机制跳出多层调用栈
        throw new Return(value);
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition)))
            execute(stmt.thenBranch);
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements,
            Environment environment) {
        // 备份上级环境
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } finally {
            // 恢复上级环境
            this.environment = previous;
        }
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        JLoxClass klass = new JLoxClass(stmt.name.lexeme);
        environment.assign(stmt.name, klass);
        return null;
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
        Integer distance = locals.get(expr);
        if (distance != null)
            environment.assginAt(distance, expr.name, value);
        else
            globals.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        // 逻辑运算符并不承诺会真正返回 true 或 false
        // 而只是保证它将返回一个具有适当真实性的值。
        Object left = evaluate(expr.left);
        // 短路运算
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            // AND
            if (!isTruthy(left))
                return left;
        }
        return evaluate(expr.right);
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
            default:
                break;
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
            default:
                break;

        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null)
            return environment.getAt(distance, name.lexeme);
        else
            return globals.get(name);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        /*
         * 对被调用者的表达式求值
         * 通常情况下，这个表达式只是一个标识符,
         * 但也可能是一个执行结果为函数的表达式
         */
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        // 防止被调函数不是可被调用的对象
        if (!(callee instanceof JLoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }
        JLoxCallable function = (JLoxCallable) callee;

        // 判断传入实参与被调函数形参数量是否一致
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);

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
