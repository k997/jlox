package com.craftinginterpreters.jlox;

import java.util.List;

class JLoxFunction implements JLoxCallable {
    private final Stmt.Function declaration;

    JLoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter,
            List<Object> arguments) {
        // 定义函数内部环境变量
        Environment environment = new Environment(interpreter.globals);

        // 为形参的参数名赋值
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnvalue) {
            return returnvalue.value;
        }
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
}