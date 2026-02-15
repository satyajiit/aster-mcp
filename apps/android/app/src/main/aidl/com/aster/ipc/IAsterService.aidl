package com.aster.ipc;

import com.aster.ipc.IAsterCallback;

interface IAsterService {
    String authenticate(String token);
    String executeCommand(String action, String paramsJson);
    void registerCallback(IAsterCallback callback);
    void unregisterCallback(IAsterCallback callback);
    List<String> getAvailableTools();
    void disconnect();
}
