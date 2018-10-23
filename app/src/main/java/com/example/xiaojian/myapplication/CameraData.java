package com.example.xiaojian.myapplication;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 摄像机数据
 */
class CameraData {

    // 设备编号（用于播放）
    private String cameraSns;
    // 设备描述，标题
    private String cameraTitle;
    // 设备快照资源地址
    private String snapshotUrl;
    // 设备状态
    private Boolean online;

    List<CameraData> getCardListFromJsonString(String jsonString) {
        List<CameraData> cards = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(jsonString).getAsJsonArray();
        for (JsonElement item : array) {
            JsonObject object = item.getAsJsonObject();
            String cameraSns = object.get("cameraSns").getAsString();
            String cameraTitle = object.get("cameraTitle").getAsString();
            String snapshotUrl = object.get("snapshotUrl").getAsString();
            Boolean isOnline = object.get("online").getAsBoolean();
            cards.add(new CameraData(cameraSns, cameraTitle, snapshotUrl, isOnline));
        }
        return cards;
    }

    String cardListToJsonString(List<CameraData> cards) {
        Gson gson = new Gson();
        return gson.toJson(cards);
    }

    CameraData() {
    }

    CameraData(String cameraSns, String cameraTitle, String snapshotUrl, Boolean isOnline) {
        this.cameraSns = cameraSns;
        this.cameraTitle = cameraTitle;
        this.snapshotUrl = snapshotUrl;
        this.online = isOnline;
    }

    CameraData(String cameraSns) {
        this.cameraSns = cameraSns;
        this.online = true;
    }

    String toJsonString(){
        Gson gson = new Gson();
        return gson.toJson(CameraData.this);
    }

    String getCameraSns() {
        return cameraSns;
    }

    String getCameraTitle() {
        return cameraTitle;
    }

    String getSnapshotUrl() {
        return snapshotUrl;
    }

    Boolean getOnline() {
        return online;
    }
}
