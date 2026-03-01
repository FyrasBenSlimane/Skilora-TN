package com.skilora;

import com.skilora.utils.NetworkUtils;

public class TestNetwork {
    public static void main(String[] args) {
        System.out.println("Detected Local IP: " + NetworkUtils.getLocalIPAddress());
    }
}
