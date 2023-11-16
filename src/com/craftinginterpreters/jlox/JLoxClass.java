package com.craftinginterpreters.jlox;

import java.util.List;

public class JLoxClass implements JLoxCallable {
    final String name;

    JLoxClass(String name) {
        this.name = name;
    }

    @Override
    public Object call(Interpreter interpreter,
            List<Object> arguments) {

        JLoxInstance instance = new JLoxInstance(this);
        return instance;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
