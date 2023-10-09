package com.craftinginterpreters.jlox;

// JLox 的 return 使用 Java 的异常机制跳出多层调用栈
class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        /*
         * 禁用不需要的JVM机制
         * RuntimeException(String message, Throwable cause,
         * boolean enableSuppression,
         * boolean writableStackTrace)
         */
        super(null, null, false, false);
        this.value = value;
    }

}
