package com.craftinginterpreters.jlox;

import java.util.List;
import java.util.Map;

public class JLoxClass implements JLoxCallable {
    final String name;
    private final Map<String, JLoxFunction> methods;

    JLoxClass(String name, Map<String, JLoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    JLoxFunction findMethod(String name) {
        if (methods.containsKey(name))
            return methods.get(name);
        return null;
    }

    @Override
    public Object call(Interpreter interpreter,
            List<Object> arguments) {

        JLoxInstance instance = new JLoxInstance(this);
        // 构造函数 init
        JLoxFunction initializer = findMethod("init");
        if (initializer != null)
            initializer.bind(instance).call(interpreter, arguments);
        return instance;
    }

    @Override
    public int arity() {
        // 有构造函数元数为构造函数的参数数量
        JLoxFunction initializer = findMethod("init");
        if (initializer != null)
            return initializer.arity();
        // 无构造函数元数为 0
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
