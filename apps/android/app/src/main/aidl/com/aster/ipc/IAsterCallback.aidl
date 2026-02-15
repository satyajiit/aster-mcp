package com.aster.ipc;

oneway interface IAsterCallback {
    void onCommandResult(String requestId, boolean success, String dataJson, String error);
    void onEvent(String eventType, String payloadJson);
}
