package com.snowcap.labelme2coco;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class CoCoHolder {
    private final File cocoFile;
    private int maxImageId = 0;
    private int maxAnnotationId = 0;
    private int maxCategoryId = 0;
    private JSONObject cocoDoc;
    private JSONArray images;
    private JSONArray annotations;
    private JSONArray categories;
    private Map<String, Integer> categoryLookup = new HashMap<>();
    private Map<String, Integer> imageLookup = new HashMap<>();
    private final boolean useFullPaths;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public CoCoHolder(File cocoFile, boolean useFullPaths) throws IOException {
        this.cocoFile = cocoFile;
        this.useFullPaths = useFullPaths;
        if (!this.cocoFile.exists()) {
            this.cocoFile.createNewFile();
            this.cocoDoc = new JSONObject();
            JSONObject info = new JSONObject();
            info.put("description", "Combined datasets of IEEE Montreal traffic Cameras, UWaterloo Autonomoose Winter Driving, University of Toronto Adverse Driving set for use in 2024-2025 Ontario Tech University Snow Detection Capstone.");
            info.put("version", "1.0");
            info.put("year", 2024);
            info.put("Contributor", "SnowCap Team");
            info.put("date_created", "2024/1109");
            info.put("url", "https://engineering.ontariotechu.ca/current-students/current-undergraduate/capstone/index.php");
            this.cocoDoc.put("info", info);
            JSONObject license = new JSONObject();
            license.put("url", "mailto:william.chamberlain@ontariotechu.net");
            license.put("id", 1);
            license.put("name", "Licensing varies, please contact for info.");
            JSONArray licenses = new JSONArray();
            licenses.put(license);
            this.cocoDoc.put("licenses", licenses);
            this.cocoDoc.put("images", new JSONArray());
            this.cocoDoc.put("annotations", new JSONArray());
            this.cocoDoc.put("categories", new JSONArray());
            PrintWriter cocoWriter = new PrintWriter(this.cocoFile);
            cocoWriter.write(this.cocoDoc.toString());
            cocoWriter.flush();
            cocoWriter.close();
            maxAnnotationId = 0;
            maxCategoryId = 0;
            maxImageId = 0;
        } else {
            Scanner scanner = new Scanner(this.cocoFile);
            scanner.useDelimiter("\\Z");
            this.cocoDoc = new JSONObject(scanner.next());
            scanner.close();
            this.images = this.cocoDoc.getJSONArray("images");
            this.annotations = this.cocoDoc.getJSONArray("annotations");
            this.categories = this.cocoDoc.getJSONArray("categories");
            for (int i = 0; i < this.images.length(); i++) {
                int val = this.images.getJSONObject(i).getInt("id");
                this.maxImageId = (this.maxImageId < val) ? val : this.maxImageId;
                this.imageLookup.put(this.images.getJSONObject(i).getString("file_name"), this.images.getJSONObject(i).getInt("id"));
            }
            for (int i = 0; i < this.annotations.length(); i++) {
                int val = this.annotations.getJSONObject(i).getInt("id");
                this.maxAnnotationId = (this.maxAnnotationId < val) ? val : this.maxAnnotationId;
            }
            for (int i = 0; i < this.categories.length(); i++) {
                int val = this.categories.getJSONObject(i).getInt("id");
                this.maxCategoryId = (this.maxCategoryId < val) ? val : this.maxCategoryId;
                this.categoryLookup.put(this.categories.getJSONObject(i).getString("name"), this.categories.getJSONObject(i).getInt("id"));
            }
        }
    }

    public void append(List<LabelMeHolder> labels) {
        for (LabelMeHolder label : labels) {
            Integer imageId;
            if (this.useFullPaths) {
                imageId = this.imageLookup.get(label.getPath());
            } else {
                Path imgPath = Path.of(label.getPath());
                imageId = this.imageLookup.get(imgPath.getFileName().toString());
            }
            if (imageId == null) {
                JSONObject newImgJson = new JSONObject();
                newImgJson.put("id", ++this.maxImageId);
                newImgJson.put("width", label.getWidth());
                newImgJson.put("height", label.getHeight());
                if (this.useFullPaths)
                    newImgJson.put("file_name", label.getPath());
                else
                    newImgJson.put("file_name", Path.of(label.getPath()).getFileName().toString());
                newImgJson.put("date_captured", DATE_FORMATTER.format(new Date()));
                imageId = newImgJson.getInt("id");
                this.images.put(newImgJson);
                this.imageLookup.put(newImgJson.getString("file_name"), imageId);
            }
            String[][] segments = label.getSegments();
            for (String[] segment : segments) {
                Integer categoryId = this.categoryLookup.get(segment[0]);
                if (categoryId == null) {
                    JSONObject newCategory = new JSONObject();
                    categoryId = ++this.maxCategoryId;
                    newCategory.put("id", categoryId);
                    newCategory.put("name", segment[0]);
                    if (segment[0].toUpperCase().contains("SNOW"))
                        newCategory.put("supercategory", "snow");
                    this.categories.put(newCategory);
                    this.categoryLookup.put(segment[0], categoryId);
                }
                JSONObject newAnnotation = new JSONObject();
                newAnnotation.put("id", ++this.maxAnnotationId);
                newAnnotation.put("category_id", categoryId);
                newAnnotation.put("image_id", imageId);
                newAnnotation.put("segmentation", new JSONArray(segment[1]));
                newAnnotation.put("bbox", new JSONArray(segment[2]));
            }
        }
    }

    public void writeDoc() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(this.cocoFile);
        writer.write(this.cocoDoc.toString());
        writer.flush();
        writer.close();
    }
}
