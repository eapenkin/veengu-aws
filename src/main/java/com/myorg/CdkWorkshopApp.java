package com.myorg;

import software.amazon.awscdk.core.App;

import java.util.Arrays;

public class CdkWorkshopApp {
    public static void main(final String argv[]) {
        App app = new App();

        new CdkWorkshopStack(app, "CdkWorkshopStack");

        app.synth();
    }
}
