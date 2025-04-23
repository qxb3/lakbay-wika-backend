package org.lakbaywika.capstone;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(4321);

        app.get("/test", ctx -> {
            ctx.result("Lakbay wika test!");
        });
    }
}