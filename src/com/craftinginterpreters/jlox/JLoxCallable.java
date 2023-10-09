package com.craftinginterpreters.jlox;

import java.util.List;

interface JLoxCallable {

    // 函数期望的参数数量
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);

}
