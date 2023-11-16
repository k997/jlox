package com.craftinginterpreters.jlox;

public class JLoxInstance {
    private JLoxClass klass;

    JLoxInstance(JLoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
