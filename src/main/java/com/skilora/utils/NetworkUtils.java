package com.skilora.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Utility class for network-related operations.
 */
public class NetworkUtils {
    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * Gets the local IP address of the machine on the network.
     * Prefers IPv4 and avoids loopback addresses.
     * 
     * @return Local IP address or "127.0.0.1" if none found.
     */
    public static String getLocalIPAddress() {
        String fallback = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isLoopbackAddress() || addr.getHostAddress().contains(":")) continue;
                    
                    String ip = addr.getHostAddress();
                    logger.info("Found candidate IP: {} on interface {}", ip, iface.getName());
                    
                    // Prioritize common LAN subnets
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                        logger.info("Selected LAN IP: {}", ip);
                        return ip;
                    }
                    
                    fallback = ip; // Keep the last one as fallback if no ideal LAN IP is found
                }
            }
        } catch (Exception e) {
            logger.error("Error detecting local IP address: {}", e.getMessage());
        }
        logger.warn("No ideal LAN IP found, using fallback: {}", fallback);
        return fallback;
    }
}
