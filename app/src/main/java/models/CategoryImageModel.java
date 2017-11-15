package models;

/**
 * Created by deepankur on 09-12-2015.
 */
public class CategoryImageModel {

    private int index;
    private Boolean isChecked;

    public String getServerUID() {
        return serverUID;
    }

    public void setServerUID(String serverUID) {
        this.serverUID = serverUID;
    }

    private String serverUID;
    private int resourceId;
    private String categoryName;



    public CategoryImageModel(int index, String categoryName, Boolean isChecked, String serverUID) {
        this.index = index;
        this.categoryName = categoryName;
        this.isChecked = isChecked;
        this.serverUID = serverUID;
    }


    public int getResourceId() {
        return resourceId;

    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Boolean getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(Boolean isChecked) {
        this.isChecked = isChecked;
    }


    public String getCategoryName() {

        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }


}
