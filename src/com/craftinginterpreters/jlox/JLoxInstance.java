package com.craftinginterpreters.jlox;

import java.util.HashMap;
import java.util.Map;

public class JLoxInstance {
    private JLoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    JLoxInstance(JLoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme))
            return fields.get(name.lexeme);
        JLoxFunction method = klass.findMethod(name.lexeme);
        if (method != null)
            return method.bind(this);

        throw new RuntimeError(
                name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
