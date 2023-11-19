package com.craftinginterpreters.jlox;

import java.util.List;
import java.util.Map;

public class JLoxClass implements JLoxCallable {
    final String name;

    // 超类
    final JLoxClass superclass;
    private final Map<String, JLoxFunction> methods;

    JLoxClass(String name, JLoxClass superclass, Map<String, JLoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    JLoxFunction findMethod(String name) {
        // 优先返回类自身的方法，其次返回超类的方法
        if (methods.containsKey(name))
            return methods.get(name);
        if (superclass != null)
            return superclass.findMethod(name);
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
