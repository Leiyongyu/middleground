package com.asinking.com.openapi.service.model;

import java.util.List;

public class EbayListRequest {

    private Integer offset;
    private Integer length;
    private List<String> storeIds;
    private List<String> siteCode;
    private List<Integer> listingStatus;
    private List<Integer> autoRestocks;
    private List<Integer> listingType;
    private Integer searchField;
    private String searchSingleValue;
    private Integer listingTimeField;
    private String listingStartTime;
    private String listingEndTime;

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public List<String> getStoreIds() {
        return storeIds;
    }

    public void setStoreIds(List<String> storeIds) {
        this.storeIds = storeIds;
    }

    public List<String> getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(List<String> siteCode) {
        this.siteCode = siteCode;
    }

    public List<Integer> getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(List<Integer> listingStatus) {
        this.listingStatus = listingStatus;
    }

    public List<Integer> getAutoRestocks() {
        return autoRestocks;
    }

    public void setAutoRestocks(List<Integer> autoRestocks) {
        this.autoRestocks = autoRestocks;
    }

    public List<Integer> getListingType() {
        return listingType;
    }

    public void setListingType(List<Integer> listingType) {
        this.listingType = listingType;
    }

    public Integer getSearchField() {
        return searchField;
    }

    public void setSearchField(Integer searchField) {
        this.searchField = searchField;
    }

    public String getSearchSingleValue() {
        return searchSingleValue;
    }

    public void setSearchSingleValue(String searchSingleValue) {
        this.searchSingleValue = searchSingleValue;
    }

    public Integer getListingTimeField() {
        return listingTimeField;
    }

    public void setListingTimeField(Integer listingTimeField) {
        this.listingTimeField = listingTimeField;
    }

    public String getListingStartTime() {
        return listingStartTime;
    }

    public void setListingStartTime(String listingStartTime) {
        this.listingStartTime = listingStartTime;
    }

    public String getListingEndTime() {
        return listingEndTime;
    }

    public void setListingEndTime(String listingEndTime) {
        this.listingEndTime = listingEndTime;
    }
}
