package com.snowcap.labelme2coco;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static ExecutorService executor;
    private static File cocoFile;
    private static File labelMeDir;
    private static boolean useFullPathNames;
    private static List<LabelMeHolder> labelMeHolders = new ArrayList<>();
    public static void main(String... argv) throws IOException {
        if (argv.length != 3) {
            System.err.println("Input arguments did not match expected count of 2.");
            System.out.println("Usage: java -jar labelme2coco.jar {CoCo document} {LabelMe document directory} {Y|N (use full path names in output)}");
            System.exit(1);
        }
        cocoFile = new File(argv[0]);
        labelMeDir = new File(argv[1]);
        useFullPathNames = argv[2].toUpperCase().startsWith("Y");
        if (!labelMeDir.isDirectory()) {
            System.err.println("Provided LabelMe document directory is not a directory.");
            System.exit(1);
        }
        executor = Executors.newWorkStealingPool();
        for (File labelMeDoc : labelMeDir.listFiles()) {
            if (!labelMeDoc.getName().endsWith(".json") && !labelMeDoc.getName().equals(cocoFile.getName())) {
                LabelMeHolder newLabelMeHolder = new LabelMeHolder(labelMeDoc);
                executor.submit(newLabelMeHolder);
            }
        }
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            executor.shutdown();
        } catch (InterruptedException e) {
            System.err.println("Waiting for executors to finish has been interrupted, attempting to continue.");
            e.printStackTrace();
        }
        CoCoHolder coCoHolder = new CoCoHolder(cocoFile, useFullPathNames);
        coCoHolder.append(labelMeHolders);
        coCoHolder.writeDoc();
        System.out.println("COMPLETE!");
    }
}
