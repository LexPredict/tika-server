package com.lexpredict.tika;

public class TagData {
    public String tagName;
    public Boolean isCdata;
    public String attributeString;
    public StringBuilder data = new StringBuilder();

    public TagData(String tagName,
                   Boolean isCdata,
                   String attributeString) {
        this.tagName = tagName;
        this.isCdata = isCdata;
        this.attributeString = attributeString;
    }

    @Override
    public String toString() {
        return "TagData{" +
                "tagName='" + tagName + '\'' +
                ", isCdata=" + isCdata +
                ", attributeString='" + attributeString + '\'' +
                ", data=" + data +
                '}';
    }
}
