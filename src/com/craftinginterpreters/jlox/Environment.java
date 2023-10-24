package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.Map;

/* 
 * 作用域是理论，环境是具体实现
 * 保存变量名及值的映射关系
 * 
 */

class Environment {

    // 链表实现不同作用域
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        // 此处的 name 意思是 variable name ， 类型为 Token
        if (values.containsKey(name.lexeme))
            return values.get(name.lexeme);
        // 如果当前环境中没有找到变量，就在外部环境中尝试。
        if (enclosing != null)
            return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // 如果变量不在此环境中，它会递归地检查外部环境。
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assginAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }
}
