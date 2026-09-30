package com.springai.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {
    ToolContext toolContext;

    @Tool(description = "计算两个数之和")
    public double sum(@ToolParam(description = "被加数") double a,
                      @ToolParam(description = "加数") double b) {
        return Math.round((a + b) * 100.0) / 100.0;
    }

    @Tool(description = "计算两个数之商")
    public double divide(@ToolParam(description = "被除数") double a,
                         @ToolParam(description = "除数") double b) {
        return Math.round((a / b) * 100.0) / 100.0;
    }

    @Tool(description = "计算 a 的 b 次方")
    public double power(@ToolParam(description = "底数") double a,
                        @ToolParam(description = "指数") double b) {
        return Math.round(Math.pow(a, b) * 100.0) / 100.0;
    }
}
