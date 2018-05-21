package org.freakz.hokan_ng_springboot.bot.io.xmpp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by Petri Airio on 17.5.2017.
 */
@Configuration
@PropertySource("file:xmpp.properties")
public class XmppConfiguration {

    @Value("${xmpp.server}")
    private String xmppServer;

    @Value("${xmpp.port}")
    private int xmppPort;

    @Value("${xmpp.username}")
    private String xmppUsername;

    @Value("${xmpp.login}")
    private String xmppLogin;

    @Value("${xmpp.password}")
    private String xmppPassword;

    @Value("${xmpp.room}")
    private String xmppRoom;

    public String getXmppServer() {
        return xmppServer;
    }

    public void setXmppServer(String xmppServer) {
        this.xmppServer = xmppServer;
    }

    public int getXmppPort() {
        return xmppPort;
    }

    public void setXmppPort(int xmppPort) {
        this.xmppPort = xmppPort;
    }

    public String getXmppUsername() {
        return xmppUsername;
    }

    public void setXmppUsername(String xmppUsername) {
        this.xmppUsername = xmppUsername;
    }

    public String getXmppLogin() {
        return xmppLogin;
    }

    public void setXmppLogin(String xmppLogin) {
        this.xmppLogin = xmppLogin;
    }

    public String getXmppPassword() {
        return xmppPassword;
    }

    public void setXmppPassword(String xmppPassword) {
        this.xmppPassword = xmppPassword;
    }

    public String getXmppRoom() {
        return xmppRoom;
    }

    public void setXmppRoom(String xmppRoom) {
        this.xmppRoom = xmppRoom;
    }
}
