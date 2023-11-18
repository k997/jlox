package com.craftinginterpreters.jlox;

import java.util.List;

class JLoxFunction implements JLoxCallable {
    private final Stmt.Function declaration;

    // 实现闭包
    private final Environment closure;

    // init 函数在实例创建时已经执行，再次手动强制返回 this
    private final boolean isInitializer;

    JLoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public Object call(Interpreter interpreter,
            List<Object> arguments) {
        // 定义函数内部环境变量
        Environment environment = new Environment(closure);

        // 为形参的参数名赋值
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnvalue) {
            // 初始化语句中执行 return 总是返回 this
            if (isInitializer)
                return closure.getAt(0, "this");
            return returnvalue.value;
        }

        // init 函数在实例创建时自动执行，用户手动执行 init 强制返回 this,
        if (isInitializer)
            return closure.getAt(0, "this");
        return null;

    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    JLoxFunction bind(JLoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new JLoxFunction(declaration, environment, isInitializer);
    }
}