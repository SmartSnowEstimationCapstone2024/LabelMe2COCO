package com.snowcap.labelme2coco;

import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LabelMeHolder implements Runnable {
    private final File labelMeJson;
    private List<String[]> segments = new LinkedList<>();
    private String path;
    private int height, width;
    private boolean passed = true;

    public LabelMeHolder(File labelMeJson) {
        this.labelMeJson = labelMeJson;
    }

    public LabelMeHolder(String labelMeJson) {
        this.labelMeJson = new File(labelMeJson);
    }

    public String getPath() {
        return this.path;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public String[][] getSegments() {
        String[][] outputArr = new String[this.segments.size()][2];
        for (int i = 0; i < outputArr.length; i++) {
            outputArr[i][0] = this.segments.get(i)[0];
            outputArr[i][1] = this.segments.get(i)[1];
        }
        return outputArr;
    }

    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(this.labelMeJson);
            scanner.useDelimiter("\\Z");
            JSONObject jObj = new JSONObject(scanner.next());
            scanner.close();
            this.path = jObj.getString("imagePath");
            this.height = jObj.getInt("imageHeight");
            this.width = jObj.getInt("imageWidth");
            JSONArray jArr = jObj.getJSONArray("shapes");
            for (int i = 0; i < jArr.length(); i++) {
                String[] segment = new String[2];
                segment[0] = jArr.getJSONObject(i).getString("label");
                segment[1] = jArr.getJSONObject(i).getJSONArray("points").toString();
            }
        } catch (FileNotFoundException | JSONException e) {
            System.err.println("Failure in processing LabelMe Json File.");
            e.printStackTrace();
            this.passed = false;
        }
    }

    public boolean isPass() {
        return this.passed;
    }
}
