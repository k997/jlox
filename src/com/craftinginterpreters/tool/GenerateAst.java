package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    // 生成抽象语法树类型，用于表示和处理编程语言中的表达式。
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        /*
         * 
         * 定义类型
         * 1. Binary（二元表达式）
         * 2. Grouping（分组表达式，括号）
         * 3. Literal（字面量表达式）
         * 4. Unary（一元表达式）。
         * 5. Variable (变量名)
         * 6. Assign （赋值)
         */
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Expression : Expr expression",
                "Print : Expr expression",
                "Var : Token name, Expr initializer"));

    }

    private static void defineAst(
            String outputDir, String baseName, List<String> types) throws IOException {

        // 定义抽象语法树的基类
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.jlox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        /*
         * 访问者模式（Visitor Pattern），它可以使得程序在处理不同类型的对象时更加灵活和可扩展。
         * 通过将具体的操作从对象结构中分离出来，我们可以轻松地添加新的操作（Visitor 的实现），
         * 而无需修改现有的对象结构（Expr 类的子类）。
         * Visitor 接口定义了多个 visitXxxExpr 方法，用于访问不同类型的表达式，并返回相应的结果 R。
         * accept 方法用于调用 Visitor 的对应方法，以便处理具体的表达式。
         * 
         */
        // 定义表达式类型接口
        defineVisitor(writer, baseName, types);
        // 实现抽象访问者接口
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        // 生成表达式类型
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);

        }

        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName,
            String className, String fieldList) {
        // 定义表达式类型
        // 与上一个类型空一行
        writer.println();

        writer.println("    static class " + className + " extends " + baseName + " {");
        // 表达式类构造函数
        writer.println("        " + className + "(" + fieldList + ") {");
        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        // 定义私有变量
        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        // 实现访问者接口
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        // 定义类型结束
        writer.println("    }");

    }

    private static void defineVisitor(
            PrintWriter writer, String baseName, List<String> types) {
        // 定义表达式类型接口
        writer.println();
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");

        }
        writer.println("    }");
    }
}